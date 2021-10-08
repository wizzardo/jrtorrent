package com.wizzardo.jrt.db;

import com.wizzardo.http.framework.di.PostConstruct;
import com.wizzardo.http.framework.di.Service;
import com.wizzardo.tools.sql.DBTools;
import com.wizzardo.tools.sql.SimpleConnectionPool;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.ConnectionPoolDataSource;

public class DBService extends DBTools implements Service, PostConstruct {

    protected ConnectionPoolDataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:~/test");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    public void init() {
        ConnectionPoolDataSource dataSource = createDataSource();
        this.dataSource = new SimpleConnectionPool(dataSource, 8);
        migrate();
    }

}
