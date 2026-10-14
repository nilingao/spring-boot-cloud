package cn.com.nla.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the checked-in administrative-division data delivery script.
 *
 * <p>Earlier rounds diffed the delivery against the legacy {@code sql/sys_area.sql}. Stage 9
 * removed that directory together with the rest of the {@code 5.0.0-dev} reference tree
 * (see mapping.md 9.2.1), and that diff had already proven the two id lists identical, so the
 * contract is now frozen from those measured values: 3,256 unique rows in strictly ascending
 * {@code area_id} order from {@code 1} to {@code 3259}, written as one explicit-column INSERT.
 * Re-validating against the legacy source is intentionally not restored — the source is gone.
 */
class SysAreaDataContractTest {

    private static final Pattern ROW_ID = Pattern.compile("(?m)^\\((\\d+),");

    private static final Pattern INSERT_HEADER = Pattern.compile("INSERT INTO sys_area \\(");

    /** Repository-relative delivery script, and the anchor used to locate the repository root. */
    private static final String DELIVERY = "script/sql/nla_system_area_data.sql";

    /** Row count and id bounds measured from the delivery script (and from the deleted legacy source). */
    private static final int EXPECTED_ROWS = 3256;

    @Test
    void deliveryRowsAreCompleteUniqueOrderedAndUseExplicitTargetColumns() throws IOException {
        Path root = findRoot();
        String delivery = Files.readString(root.resolve(DELIVERY), StandardCharsets.UTF_8);

        List<String> ids = ids(delivery);
        assertEquals(EXPECTED_ROWS, ids.size(), "row count changed without re-validating the import");
        assertEquals(EXPECTED_ROWS, (int) ids.stream().distinct().count(), "duplicate area_id in the delivery script");
        assertEquals("1", ids.get(0), "first area_id");
        assertEquals("3259", ids.get(ids.size() - 1), "last area_id");
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(Long.parseLong(ids.get(i)) > Long.parseLong(ids.get(i - 1)),
                "rows must stay in ascending area_id order, broke at index " + i);
        }

        assertEquals(1, INSERT_HEADER.matcher(delivery).results().count(),
            "the delivery is a single multi-row INSERT; splitting it changes the import order contract");
        assertTrue(delivery.startsWith("-- sys_area reference data for nla-system"));
        assertTrue(delivery.contains("INSERT INTO sys_area (area_id, area_code, area_name, level, city_code, center, parent_id) VALUES"));
        assertTrue(delivery.trim().endsWith(";"));
    }

    private static List<String> ids(String sql) {
        Matcher matcher = ROW_ID.matcher(sql);
        var ids = new ArrayList<String>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    private static Path findRoot() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null) {
            if (Files.isRegularFile(path.resolve(DELIVERY))) {
                return path;
            }
            path = path.getParent();
        }
        throw new IllegalStateException("repository root not found");
    }
}
