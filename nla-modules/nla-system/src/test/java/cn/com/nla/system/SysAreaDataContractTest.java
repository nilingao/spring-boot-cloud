package cn.com.nla.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the checked-in administrative-division data delivery script. */
class SysAreaDataContractTest {

    private static final Pattern ROW_ID = Pattern.compile("(?m)^\\((\\d+),");

    @Test
    void deliveryRowsMatchSourceRowsAndUseExplicitTargetColumns() throws IOException {
        Path root = findRoot();
        String source = Files.readString(root.resolve("sql/sys_area.sql"), StandardCharsets.UTF_8);
        String delivery = Files.readString(root.resolve("script/sql/nla_system_area_data.sql"), StandardCharsets.UTF_8);

        var sourceIds = ids(source);
        var deliveryIds = ids(delivery);
        assertEquals(sourceIds, deliveryIds);
        assertEquals(3256, deliveryIds.size());
        assertTrue(delivery.startsWith("-- sys_area reference data for nla-system"));
        assertTrue(delivery.contains("INSERT INTO sys_area (area_id, area_code, area_name, level, city_code, center, parent_id) VALUES"));
        assertTrue(delivery.trim().endsWith(";"));
    }

    private static java.util.List<String> ids(String sql) {
        Matcher matcher = ROW_ID.matcher(sql);
        var ids = new java.util.ArrayList<String>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    private static Path findRoot() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null) {
            if (Files.isRegularFile(path.resolve("sql/sys_area.sql"))) {
                return path;
            }
            path = path.getParent();
        }
        throw new IllegalStateException("repository root not found");
    }
}
