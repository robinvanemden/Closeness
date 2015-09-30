package io.pavlov.closeness;

import de.triplet.simpleprovider.AbstractProvider;
import de.triplet.simpleprovider.Column;
import de.triplet.simpleprovider.Table;

public class KeyValueProvider extends AbstractProvider {

    @Override
    protected String getAuthority() {
        return "io.pavlov.closeness.KEYVAL";
    }

    @Table
    public class KeyValue {

        @Column(value = Column.FieldType.INTEGER, primaryKey = true)
        public static final String KEY_ID = "_id";

        @Column(value = Column.FieldType.TEXT)
        public static final String KEY = "key";

        @Column(value = Column.FieldType.TEXT)
        public static final String VALUE = "value";

        @Column(value = Column.FieldType.TEXT)
        public static final String EXT = "ext";

        @Column(value = Column.FieldType.TEXT)
        public static final String TYPE = "type";

        @Column(value = Column.FieldType.TEXT, since = 3)
        public static final String ADDRESS = "address";

        @Column(value = Column.FieldType.TEXT, since = 4)
        public static final String ANDROID_ID = "android_id";

        @Column(value = Column.FieldType.TEXT, since = 5)
        public static final String DEVICE_ID = "device_id";

        @Column(value= Column.FieldType.TEXT)
        public static final String SYSTEMTIME = "systemtime";

        @Column(value = Column.FieldType.INTEGER, since = 2)
        public static final String STATUSLOCAL = "statuslocal"; // 0 is ok, 1 is dirty (change or new), 2 is deleted
    }

    @Override
    protected int getSchemaVersion() {
        return 6;
    }

}
