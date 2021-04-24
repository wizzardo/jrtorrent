package com.wizzardo.jrt.db;

import com.wizzardo.http.framework.di.PostConstruct;
import com.wizzardo.http.framework.di.Service;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.security.MD5;
import com.wizzardo.jrt.db.query.QueryBuilder;
import com.wizzardo.jrt.db.query.Table;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.ConnectionPoolDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class DBService implements Service, PostConstruct {

    public interface Consumer<T> {
        void consume(T var1) throws SQLException;
    }

    public interface Mapper<T, R> {
        R map(T var1) throws SQLException;
    }

    private ConnectionPoolDataSource dataSource;
    private MiniConnectionPoolManager poolMgr;

    protected Connection createConnection() throws SQLException {
        return poolMgr.getConnection();
    }

    public <R> R withDB(Unchecked.Consumer<Connection, R> mapper) {
        try (Connection connection = createConnection()) {
            return mapper.call(connection);
        } catch (Exception e) {
            throw Unchecked.rethrow(e);
        }
    }

    public <R> R withBuilder(Mapper<QueryBuilder.WrapConnectionStep, R> mapper) {
        try (Connection connection = createConnection()) {
            return mapper.map(QueryBuilder.withConnection(connection));
        } catch (Exception e) {
            throw Unchecked.rethrow(e);
        }
    }

    public void consume(Consumer<QueryBuilder.WrapConnectionStep> consumer) {
        try (Connection connection = createConnection()) {
            consumer.consume(QueryBuilder.withConnection(connection));
        } catch (Exception e) {
            throw Unchecked.rethrow(e);
        }
    }

    public static class SchemaHistory {
        final int id;
        final String name;
        final Date dateExecuted;
        final String md5;

        public SchemaHistory(int id, String name, Date dateExecuted, String md5) {
            this.id = id;
            this.name = name;
            this.dateExecuted = dateExecuted;
            this.md5 = md5;
        }
    }


    protected ConnectionPoolDataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:~/test");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    public void init() {
        dataSource = createDataSource();
        poolMgr = new MiniConnectionPoolManager(dataSource, 8);

//        sqllite
//        withDB(connection -> connection.createStatement().executeUpdate("create table if not exists schema_history( id INTEGER PRIMARY KEY, name VARCHAR(128), date_executed DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), md5 CHAR(32));"));

//        withDB(connection -> connection.createStatement().executeUpdate("drop table if exists schema_history;"));
        withDB(connection -> connection.createStatement().executeUpdate("create table if not exists schema_history( id IDENTITY, name VARCHAR(128), date_executed timestamp default now(), md5 CHAR(32));"));

        List<SchemaHistory> executedMigrations = withDB(c -> {
            ResultSet rs = c.createStatement().executeQuery("select * from schema_history");
            List<SchemaHistory> l = new ArrayList<>();
            while (rs.next()) {
                l.add(new SchemaHistory(rs.getInt("id"), rs.getString("name"), rs.getTimestamp("date_executed"), rs.getString("md5")));
            }
            return l;
        });

        List<String> migrations = Arrays.asList(getResourceAsString("/migrations.txt").split("\n"));
        Collections.sort(migrations);
        migrations.forEach(name -> {
//            System.out.println(s);
//            System.out.println(Unchecked.call(() -> getResourceAsString(s)));

            String migration = getResourceAsString(name);
            String md5 = MD5.create().update(migration).asString();

            Optional<SchemaHistory> executed = executedMigrations.stream().filter(it -> it.name.equals(name)).findFirst();
            if (executed.isPresent()) {
                if (!executed.get().md5.equals(md5))
                    throw new IllegalStateException("Migration " + name + " has different md5!");
                return;
            }

//            System.out.println("executing migration " + name);

            withDB(c -> {
                c.setAutoCommit(false);
                try {
                    c.prepareStatement(migration).executeUpdate();
                    c.prepareStatement("insert into schema_history (name, md5) values('" + name + "', '" + md5 + "')").executeUpdate();
                    c.commit();
                } catch (Exception e) {
                    System.out.println("Cannot execute migration: " + name);
                    System.out.println(migration);
                    e.printStackTrace();
                    c.rollback();
                }
                return true;
            });
        });
    }

    public long getLastInsertedId(Connection c) throws SQLException {
        ResultSet rs = c.prepareStatement("select last_insert_rowid()").executeQuery();
        rs.next();
        return rs.getLong(1);
    }

    public long insertInto(Object o, Table t) throws SQLException {
        return withBuilder(c -> insertInto(c, o, t));
    }

    public long insertInto(QueryBuilder.WrapConnectionStep c, Object o, Table t) throws SQLException {
        return c.insertInto(t).values(o).executeInsert();
//        return getLastInsertedId(c.getConnection());
    }

    private static String getResourceAsString(String name) {
        if (!name.startsWith("/"))
            name = "/" + name;

        String finalName = name;
        byte[] bytes;
        bytes = Unchecked.ignore(() -> IOTools.bytes(DBService.class.getResourceAsStream(finalName)));
        int i = 0;
        while (bytes == null) {
            i++;
            if (i > 5)
                throw new RuntimeException("Cannot read resource " + finalName);
            Unchecked.ignore(() -> Thread.sleep(100));
            bytes = Unchecked.ignore(() -> IOTools.bytes(DBService.class.getResourceAsStream(finalName)));
        }
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }
}
