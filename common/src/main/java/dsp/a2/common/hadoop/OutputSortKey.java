package dsp.a2.common.hadoop;

import dsp.a2.common.OutputLine;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Optional helper for the FINAL sort step: a key whose natural order is exactly the contract order of the
 * output ({@link OutputLine#ORDER}: w1, w2 ascending; probability descending; w3 ascending).
 *
 * <p>Using it is not mandatory – each implementation designs its own sort step – but if both use it,
 * the order is guaranteed to be identical.
 *
 * <p>NOTE on partitioning: {@link #hashCode()} uses only (w1, w2), so with the default HashPartitioner all
 * completions of a pair go to the same reducer, but the part files are NOT globally ordered. Global order
 * requires a range partitioner (e.g. TotalOrderPartitioner) or a single reducer for the sort step.
 */
public class OutputSortKey implements WritableComparable<OutputSortKey> {

    private String w1 = "";
    private String w2 = "";
    private String w3 = "";
    private double probability;

    /** Needed by Hadoop (reflection). */
    public OutputSortKey() {
    }

    public OutputSortKey(String w1, String w2, String w3, double probability) {
        set(w1, w2, w3, probability);
    }

    public final void set(String w1, String w2, String w3, double probability) {
        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
        this.probability = probability;
    }

    public String w1() {
        return w1;
    }

    public String w2() {
        return w2;
    }

    public String w3() {
        return w3;
    }

    public double probability() {
        return probability;
    }

    public OutputLine toOutputLine() {
        return new OutputLine(w1, w2, w3, probability);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        Text.writeString(out, w1);
        Text.writeString(out, w2);
        Text.writeString(out, w3);
        out.writeDouble(probability);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        w1 = Text.readString(in);
        w2 = Text.readString(in);
        w3 = Text.readString(in);
        probability = in.readDouble();
    }

    @Override
    public int compareTo(OutputSortKey o) {
        return OutputLine.compare(w1, w2, w3, probability, o.w1, o.w2, o.w3, o.probability);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OutputSortKey)) {
            return false;
        }
        return compareTo((OutputSortKey) o) == 0;
    }

    /** Only (w1, w2): consistent with equals, and keeps all completions of a pair on one reducer. */
    @Override
    public int hashCode() {
        return 31 * w1.hashCode() + w2.hashCode();
    }

    @Override
    public String toString() {
        return toOutputLine().format();
    }
}
