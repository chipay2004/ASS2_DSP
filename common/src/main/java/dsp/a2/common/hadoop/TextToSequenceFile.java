package dsp.a2.common.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Converts a local text file in the dataset line format into a SequenceFile of
 * {@code (LongWritable rowNumber, Text line)} – the same layout as the Google N-gram dataset on S3
 * (without LZO). Lets you test {@code --input-format sequence} locally before paying for EMR.
 *
 * <pre>
 *   hadoop jar common/target/common.jar dsp.a2.common.hadoop.TextToSequenceFile data/test-input.txt /tmp/test-input.seq
 * </pre>
 */
public final class TextToSequenceFile {

    private TextToSequenceFile() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: TextToSequenceFile <input.txt> <output.seq>");
            System.exit(2);
        }
        long rows = convert(new Configuration(), args[0], new Path(args[1]));
        System.out.println("Wrote " + rows + " rows to " + args[1]);
    }

    /** @return number of rows written */
    public static long convert(Configuration conf, String localTextFile, Path output) throws IOException {
        long row = 0;
        try (BufferedReader in = Files.newBufferedReader(Paths.get(localTextFile), StandardCharsets.UTF_8);
             SequenceFile.Writer writer = SequenceFile.createWriter(conf,
                     SequenceFile.Writer.file(output),
                     SequenceFile.Writer.keyClass(LongWritable.class),
                     SequenceFile.Writer.valueClass(Text.class),
                     SequenceFile.Writer.compression(SequenceFile.CompressionType.NONE))) {
            LongWritable key = new LongWritable();
            Text value = new Text();
            String line;
            while ((line = in.readLine()) != null) {
                key.set(row++);
                value.set(line);
                writer.append(key, value);
            }
        }
        return row;
    }
}
