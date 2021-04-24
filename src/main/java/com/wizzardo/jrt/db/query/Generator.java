package com.wizzardo.jrt.db.query;

import com.wizzardo.jrt.db.model.TorrentBinary;
import com.wizzardo.jrt.db.model.TorrentEntryPriority;
import com.wizzardo.jrt.db.model.TorrentInfo;
import com.wizzardo.tools.collections.flow.Flow;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.reflection.FieldInfo;
import com.wizzardo.tools.reflection.Fields;

import java.io.File;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Generator {

    final File root;
    final String packag;

    public Generator(String generatedFilesRoot, String packag) {
        root = new File(generatedFilesRoot);
        this.packag = packag;
        if (!root.exists()) {
            root.mkdirs();
        }
    }

    void createTableFor(Class cl) {
        Fields<FieldInfo> fieldInfos = new Fields<>(cl);
        StringBuilder sb = new StringBuilder();
        String classTableName = cl.getSimpleName() + "Table";
        String offset = "    ";
        sb.append("package ").append(packag).append(";\n");
        sb.append("import com.wizzardo.jrt.db.query.*;\n\n");
        sb.append("import java.util.Arrays;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Collections;\n");

        List<FieldDescription> fields = Flow.of(fieldInfos.iterator())
                .map(fieldInfo -> new FieldDescription(getFieldType(fieldInfo.field.getType()), fieldInfo.field.getType(), fieldInfo))
                .toList()
                .get();
        fields.forEach(d -> sb.append(d.importString()));

        sb.append("public class ").append(classTableName).append(" extends Table {\n\n");
        sb.append(offset).append("private ").append(classTableName).append("(String name, String alias) {\n").append(offset).append(offset).append("super(name, alias);\n").append(offset).append("}\n\n");
        sb.append(offset).append("public ").append(classTableName).append(" as(String alias) {\n").append(offset).append(offset).append("return new ").append(classTableName).append("(name, alias);\n").append(offset).append("}\n\n");


        sb.append(offset).append("public final static ").append(classTableName).append(" INSTANCE")
                .append(" = new ").append(classTableName).append("(\"").append(toSqlName(cl.getSimpleName())).append("\", null);\n\n");

        fields.forEach(d -> {
            sb.append(offset).append("public final Field.").append(d.toString()).append(" ").append(toSqlName(d.fieldInfo.field.getName()).toUpperCase())
                    .append(" = new Field.").append(d.toString()).append("(this, \"").append(toSqlName(d.fieldInfo.field.getName())).append("\")")
                    .append(";\n");
        });

        sb.append("\n");

        sb.append(offset).append("public final List<Field> FIELDS").append(" ").append(" = Collections.unmodifiableList(Arrays.asList(");
        fieldInfos.each(fieldInfo -> sb.append(toSqlName(fieldInfo.field.getName()).toUpperCase()).append(", "));
        sb.setLength(sb.length() - 2);
        sb.append("));\n");

        sb.append(offset).append("public List<Field> getFields() {\n");
        sb.append(offset).append(offset).append("return FIELDS;\n");
        sb.append(offset).append("}\n");


        sb.append("}");

        setTextIfChanged(classTableName, sb.toString());
    }

    protected void setTextIfChanged(String classTableName, String content) {
        File file = new File(root, classTableName + ".java");
        if (!file.exists() || !FileTools.text(file).equals(content))
            FileTools.text(file, content);
    }

    protected static String toSqlName(String name) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return name.replaceAll(regex, replacement).toLowerCase();
    }

    static class FieldDescription<T> {
        final Class<Field> fieldClass;
        final Class<T> fieldType;
        final FieldInfo fieldInfo;

        FieldDescription(Class<Field> fieldClass, Class<T> fieldType, FieldInfo fieldInfo) {
            this.fieldClass = fieldClass;
            this.fieldType = fieldType;
            this.fieldInfo = fieldInfo;
        }

        public String importString() {
            if (fieldType.isEnum()) {
                return "import " + fieldType.getCanonicalName() + ";\n";
            }
            return "";
        }

        @Override
        public String toString() {
            if (fieldType.isEnum()) {
                return fieldClass.getSimpleName() + "<" + fieldType.getSimpleName() + ">";
            }
            return fieldClass.getSimpleName();
        }
    }

    protected Class getFieldType(Class cl) {
        if (cl.isArray()) {
            Class componentType = cl.getComponentType();
            if (!componentType.isPrimitive())
                throw new IllegalArgumentException("Only primitive arrays are supported for now");
            if (componentType == byte.class) {
                return Field.ByteArrayField.class;
            }

            throw new IllegalArgumentException("Only byte arrays are supported for now");
        }
        if (cl.isPrimitive()) {
            if (cl == long.class)
                return Field.LongField.class;
            if (cl == int.class)
                return Field.IntField.class;
            if (cl == short.class)
                return Field.ShortField.class;
            if (cl == byte.class)
                return Field.ByteField.class;
            if (cl == float.class)
                return Field.FloatField.class;
            if (cl == double.class)
                return Field.DoubleField.class;
            if (cl == boolean.class)
                return Field.BooleanField.class;
        } else if (cl == String.class)
            return Field.StringField.class;
        else if (cl == Long.class)
            return Field.LongField.class;
        if (cl == Integer.class)
            return Field.IntField.class;
        if (cl == Short.class)
            return Field.ShortField.class;
        if (cl == Byte.class)
            return Field.ByteField.class;
        if (cl == Float.class)
            return Field.FloatField.class;
        if (cl == Double.class)
            return Field.DoubleField.class;
        if (cl == Boolean.class)
            return Field.BooleanField.class;
        if (cl.isEnum())
            return Field.EnumField.class;
        else if (cl == Date.class)
            return Field.DateField.class;
        else if (cl == Timestamp.class)
            return Field.TimestampField.class;

        throw new IllegalArgumentException("Cannot find proper field-class for class " + cl);
    }

    private void createTables(Class... classes) {

        StringBuilder sb = new StringBuilder();
        String classTableName = "Tables";
        String offset = "    ";
        sb.append("package ").append(packag).append(";\n");
        sb.append("import com.wizzardo.jrt.db.query.*;\n\n");
        sb.append("public class ").append(classTableName).append(" {\n\n");

        Arrays.asList(classes).forEach(cl -> {
            sb.append(offset).append("public final static ").append(cl.getSimpleName()).append("Table ").append(toSqlName(cl.getSimpleName()).toUpperCase()).append(" = ").append(cl.getSimpleName()).append("Table.INSTANCE").append(";\n");
        });
        sb.append("}");

        setTextIfChanged(classTableName, sb.toString());

        for (Class aClass : classes) {
            createTableFor(aClass);
        }
    }


    public static void main(String[] args) {
        Generator generator = new Generator("src/main/java/com/wizzardo/jrt/db/generated", "com.wizzardo.jrt.db.generated");
        generator.createTables(TorrentInfo.class, TorrentBinary.class, TorrentEntryPriority.class);
    }

}
