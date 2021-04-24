package bt.data;

import java.util.List;

public class ReadWriteDataRangeHelper {

    static public ReadWriteDataRange create(List<StorageUnit> units, long offsetInFirstUnit, long limitInLastUnit) {
        return new ReadWriteDataRange(units, offsetInFirstUnit, limitInLastUnit);
    }
}
