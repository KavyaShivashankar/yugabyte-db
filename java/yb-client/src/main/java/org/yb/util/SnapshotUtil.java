package org.yb.util;

import static org.yb.util.HybridTimeUtil.HTTimestampToPhysicalAndLogical;

import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import org.yb.annotations.InterfaceAudience;
import org.yb.client.SnapshotInfo;
import org.yb.client.SnapshotRestorationInfo;
import org.yb.master.CatalogEntityInfo;
import org.yb.master.MasterBackupOuterClass.SnapshotInfoPB;
import org.yb.master.MasterBackupOuterClass.SysRestorationEntryPB;
import org.yb.master.MasterBackupOuterClass.RestorationInfoPB;
import org.yb.annotations.InterfaceAudience;

@InterfaceAudience.Private
public class SnapshotUtil {

    public static UUID convertToUUID(@Nullable ByteString byteString) {
        if (Objects.isNull(byteString) || byteString.isEmpty()) {
          return null;
        }
        byte[] bytes = byteString.toByteArray();
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public static ByteString convertToByteString(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return ByteString.copyFrom(bb.array());
    }

    public static SnapshotInfo parseSnapshotInfoPB(SnapshotInfoPB snapshotInfoPB) {
        UUID snapshotUUID = convertToUUID(snapshotInfoPB.getId());
        CatalogEntityInfo.SysSnapshotEntryPB snapshotEntry = snapshotInfoPB.getEntry();
        long snapshotTimeInMillis =
            HTTimestampToPhysicalAndLogical(snapshotEntry
                .getSnapshotHybridTime())[0]/1000L;
        long previousSnapshotTimeInMillis =
                snapshotEntry.hasPreviousSnapshotHybridTime() ?
                    HTTimestampToPhysicalAndLogical(snapshotEntry
                        .getPreviousSnapshotHybridTime())[0]/1000L : snapshotTimeInMillis;
        CatalogEntityInfo.SysSnapshotEntryPB.State state = snapshotEntry.getState();
        SnapshotInfo snapshotInfo =
            new SnapshotInfo(snapshotUUID, snapshotTimeInMillis,
                            previousSnapshotTimeInMillis, state);
        return snapshotInfo;
    }

    public static SnapshotRestorationInfo parseSnapshotRestorationInfoPB(
        RestorationInfoPB restorationInfoPB) {
        UUID restorationUUID = convertToUUID(restorationInfoPB.getId());
        SysRestorationEntryPB restorationEntry = restorationInfoPB.getEntry();
        UUID snapshotUUID = convertToUUID(restorationEntry.getSnapshotId());
        UUID scheduleUUID = convertToUUID(restorationEntry.getScheduleId());
        long restoreTime = HTTimestampToPhysicalAndLogical(
                            restorationEntry.getRestoreAtHt())[0]/1000L;
        long completionTime = HTTimestampToPhysicalAndLogical(
                            restorationEntry.getCompleteTimeHt())[0]/1000L;
        CatalogEntityInfo.SysSnapshotEntryPB.State state = restorationEntry.getState();
        SnapshotRestorationInfo snapshotRestorationInfo =
            new SnapshotRestorationInfo(
                    restorationUUID,
                    snapshotUUID,
                    scheduleUUID,
                    restoreTime,
                    completionTime,
                    state);
        return snapshotRestorationInfo;
    }
}
