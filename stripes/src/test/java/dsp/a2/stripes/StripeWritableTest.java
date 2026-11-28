package dsp.a2.stripes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class StripeWritableTest {

    private static StripeWritable roundTrip(StripeWritable in, StripeWritable reused) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        in.write(new DataOutputStream(bytes));
        reused.readFields(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        return reused;
    }

    @Test
    void roundTripKeepsCells() throws IOException {
        StripeWritable s = new StripeWritable();
        s.add("strong", 3, 0);
        s.add("hot", 0, 5_000_000_000L);
        StripeWritable back = roundTrip(s, new StripeWritable());
        assertEquals(2, back.size());
        assertArrayEquals(new long[] {3, 0}, back.get("strong"));
        assertArrayEquals(new long[] {0, 5_000_000_000L}, back.get("hot"));
    }

    @Test
    void addSumsSameWord() {
        StripeWritable s = new StripeWritable();
        s.add("strong", 2, 0);
        s.add("strong", 0, 4);
        s.add("strong", 1, 1);
        assertArrayEquals(new long[] {3, 5}, s.get("strong"));
    }

    @Test
    void mergeAddsCellwise() {
        StripeWritable a = new StripeWritable();
        a.add("x", 1, 2);
        a.add("y", 1, 0);
        StripeWritable b = new StripeWritable();
        b.add("x", 10, 20);
        b.add("z", 0, 7);
        a.merge(b);
        assertArrayEquals(new long[] {11, 22}, a.get("x"));
        assertArrayEquals(new long[] {1, 0}, a.get("y"));
        assertArrayEquals(new long[] {0, 7}, a.get("z"));
        assertEquals(3, a.size());
    }

    @Test
    void emptyStripeAndReuseDoNotKeepOldCells() throws IOException {
        StripeWritable reused = new StripeWritable();
        reused.add("old", 9, 9);
        StripeWritable back = roundTrip(new StripeWritable(), reused);
        assertEquals(0, back.size());
        assertNull(back.get("old"));
    }

    @Test
    void utf8WordsSurvive() throws IOException {
        StripeWritable s = new StripeWritable();
        s.add("שלום", 1, 2);
        s.add("café", 3, 4);
        StripeWritable back = roundTrip(s, new StripeWritable());
        assertArrayEquals(new long[] {1, 2}, back.get("שלום"));
        assertArrayEquals(new long[] {3, 4}, back.get("café"));
    }

    @Test
    void toStringIsSortedByWord() {
        StripeWritable s = new StripeWritable();
        s.add("b", 1, 0);
        s.add("a", 0, 2);
        assertEquals("{a:0/2, b:1/0}", s.toString());
    }
}
