package dsp.a2.stripes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

/**
 * A "stripe" for one word pair (w1 w2): a small table  w3 -> (c0, c1)  where
 * c0 = occurrences of the trigram "w1 w2 w3" in corpus part 0, c1 = occurrences in part 1.
 *
 * <p>This is the table H of the lecture's Stripes pseudo-code (Map: H{w2} := H{w2} + 1; Reduce: Hsum),
 * except that every cell holds two counters (one per corpus part) because Deleted Estimation needs r0 and r1.
 *
 * <p>Serialization is hand-written (vints, no class names) because the size of this object is what the
 * shuffle statistics measure; MapWritable would add a class name to every value.
 */
public class StripeWritable implements Writable {

    private final TreeMap<String, long[]> cells = new TreeMap<>();

    public void clear() {
        cells.clear();
    }

    /** Adds c0 / c1 occurrences to the cell of w3 (creating it if needed). */
    public void add(String w3, long c0, long c1) {
        long[] cell = cells.get(w3);
        if (cell == null) {
            cells.put(w3, new long[] {c0, c1});
        } else {
            cell[0] += c0;
            cell[1] += c1;
        }
    }

    /** Adds every cell of {@code other} into this stripe (the "add(Hsum, H)" of the lecture's Reducer). */
    public void merge(StripeWritable other) {
        for (Map.Entry<String, long[]> e : other.cells.entrySet()) {
            add(e.getKey(), e.getValue()[0], e.getValue()[1]);
        }
    }

    public int size() {
        return cells.size();
    }

    /** The cell {c0, c1} of w3, or null. Do not modify the returned array. */
    public long[] get(String w3) {
        return cells.get(w3);
    }

    /** Cells in w3 order (sorted, so output is deterministic). Read-only view. */
    public Set<Map.Entry<String, long[]>> entries() {
        return Collections.unmodifiableMap(cells).entrySet();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        WritableUtils.writeVInt(out, cells.size());
        for (Map.Entry<String, long[]> e : cells.entrySet()) {
            Text.writeString(out, e.getKey());
            WritableUtils.writeVLong(out, e.getValue()[0]);
            WritableUtils.writeVLong(out, e.getValue()[1]);
        }
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        cells.clear(); // Hadoop reuses Writable objects between records
        int n = WritableUtils.readVInt(in);
        for (int i = 0; i < n; i++) {
            String w3 = Text.readString(in);
            long c0 = WritableUtils.readVLong(in);
            long c1 = WritableUtils.readVLong(in);
            cells.put(w3, new long[] {c0, c1});
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, long[]> e : cells.entrySet()) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append(':').append(e.getValue()[0]).append('/').append(e.getValue()[1]);
        }
        return sb.append('}').toString();
    }
}
