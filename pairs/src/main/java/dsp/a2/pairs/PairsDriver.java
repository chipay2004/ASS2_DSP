package dsp.a2.pairs;

import dsp.a2.common.RunParameters;
import dsp.a2.common.hadoop.JobSetup;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Entry point of the PAIRS implementation (Partner A). SKELETON ONLY – the algorithm is not implemented.
 *
 * <p>Command line (shared contract, see {@link RunParameters}):
 * <pre>
 *   hadoop jar pairs-job.jar [-D key=value ...] &lt;input&gt; &lt;outputBase&gt; --local-aggregation true|false
 *          [--input-format sequence|text] [--reducers N] [--stopwords PATH]
 * </pre>
 * The final knowledge base must be written to {@code new OutputLayout(outputBase).finalDir()} and each
 * step's statistics through {@code StepRunner.run(job, stepName, layout.statsDir())}.
 *
 * <p>See docs/PAIRS_TASKS.md for the work plan and docs/SHARED_CONTRACT.md for the rules.
 */
public class PairsDriver extends Configured implements Tool {

    public static final String IMPLEMENTATION = "pairs";

    public static void main(String[] args) throws Exception {
        System.exit(ToolRunner.run(new Configuration(), new PairsDriver(), args));
    }

    @Override
    public int run(String[] args) throws Exception {
        RunParameters params;
        try {
            params = RunParameters.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println(RunParameters.usage("PairsDriver"));
            return 2;
        }
        Configuration conf = getConf();
        JobSetup.apply(conf, params, IMPLEMENTATION);
        System.out.println("[" + IMPLEMENTATION + "] " + params);

        // TODO(Partner A): build and run the Pairs MapReduce steps here (docs/PAIRS_TASKS.md):
        //   step 1 reads the corpus: JobSetup.configureCorpusInput(job, params) + CorpusFilter + CommonCounters
        //   N = CommonCounters.value(job1.getCounters(), CommonCounters.N_INSTANCES); JobSetup.setCorpusN(conf, N)
        //   every step: StepRunner.run(job, "stepK-name", params.layout().statsDir())
        //   last step writes params.layout().finalDir() in the contract output format, globally sorted.
        throw new UnsupportedOperationException(
                "The Pairs implementation is not written yet - see docs/PAIRS_TASKS.md");
    }
}
