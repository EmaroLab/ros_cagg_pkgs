package it.emarolab.ros_cagg_node;

import org.apache.commons.logging.Log;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;
import org.ros.message.MessageListener;
import org.ros.node.topic.Subscriber;

import java.lang.*;
import java.util.List;
import java.util.ArrayList;

// Node name: "CAGG_node"
// Listens a String at: /CAGG/input_text/
// Reply with a String[String[]] at: /CAGG/semantic_tags/
public class CaggNode extends AbstractNodeMain {

    // required parameters
    public static final String PARAM_NAME_LOG_CONFIG = "/cagg_log_config_path";
    public static final String PARAM_NAME_GRAMMAR = "/cagg_serialized_grammar";
    /* examples
    public static final String GRAMMAR_PATH = ..."ros_cagg_pkgs/ros_cagg_node/ros_java_cagg_node_interface/res/grammar.ser";
    public static final String LOG_CONFIG_PATH = ..."ros_cagg_node/ros_java_cagg_node_interface/res/log4j_guiConf.xml";
    */

    // auxiliary parameter
    public static final String PARAM_NAME_STOPPING_CONFIDENCE = "/cagg_stopping_confidence_threshold";
    public static final String PARAM_NAME_STOPIING_CHECK_FREQUENCY = "/cagg_stopping_check_frequency";
    public static final String PARAM_NAME_CAGG_TIMEOUT_MS = "/cagg_timeout_ms";
    public static final int DEFAULT_CAGG_TIMEOUT = 10000;//in millisec
    public static final double DEFAULT_STOPPING_THRESHOLD = .2f; // [0,1]
    public static final int DEFAULT_CHEKING_FREQUENCY = 1000; // in millisec

    

    public static final long SPINNING_RATE = 1000;

    // to be published at the next spin, set different to null in the callback (reset to nul after publishing)
    private List< CaggInterface.ROSResult> semanticTags = new ArrayList<>();
    private CaggInterface cagg;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("CAGG_node");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        final Log log = connectedNode.getLog();
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber("/CAGG/input_text/", std_msgs.String._TYPE);

	// requires those paramerers!!! default not set
        ParameterTree params = connectedNode.getParameterTree();
        String logFile = params.getString( PARAM_NAME_LOG_CONFIG, "");
        String grammarFile = params.getString( PARAM_NAME_GRAMMAR, "");

	int chekingFrequency = params.getInteger( PARAM_NAME_STOPIING_CHECK_FREQUENCY,DEFAULT_CHEKING_FREQUENCY);
        double stoppingThreshold = params.getDouble( PARAM_NAME_STOPPING_CONFIDENCE, DEFAULT_STOPPING_THRESHOLD);
        int timeout = params.getInteger( PARAM_NAME_CAGG_TIMEOUT_MS, DEFAULT_CAGG_TIMEOUT);
        cagg = new CaggInterface(logFile, grammarFile, timeout, chekingFrequency, stoppingThreshold);

	// SUBSCRIBER callback
        subscriber.addMessageListener(message -> {
            synchronized (this) {
		// apply directive grammar: "go" | "sop" | "reset"
		CaggInterface.ROSResult result = cagg.evaluate( message.getData());
		semanticTags.add( result);
            }
        });

        // publisher and spin loop
        final Publisher<ros_cagg_msgs.cagg_tags> caggEvaluationPublisher = connectedNode.newPublisher("/CAGG/semantic_tags/", ros_cagg_msgs.cagg_tags._TYPE);
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int sequenceNumber;
            @Override
            protected void setup() {sequenceNumber = 0;}

            @Override
            protected void loop() throws InterruptedException {
                sequenceNumber++;
                log.info("looping");

// publish only if data is available and clean semanticTags msg
                synchronized (this) {
                    // publish a String[String[]] created in the callback
                    for( CaggInterface.ROSResult st : semanticTags){
                        ros_cagg_msgs.cagg_tags caggEvaluationMsg = caggEvaluationPublisher.newMessage();
                        caggEvaluationMsg.setConfidence( st.getConfidence());

                        List<ros_cagg_msgs.cagg_tag> tagsMsg = new ArrayList<>();
                        for (List<String> word : st.getTags()) {
                            ros_cagg_msgs.cagg_tag caggSemanticTagsMsg = connectedNode.getTopicMessageFactory().newFromType(ros_cagg_msgs.cagg_tag._TYPE);
                            caggSemanticTagsMsg.setCaggTag(word);
                            tagsMsg.add(caggSemanticTagsMsg);
                        }

                        caggEvaluationMsg.setCaggTags(tagsMsg);
                        Time time = Time.fromMillis(System.currentTimeMillis());
                        caggEvaluationMsg.getHeader().setStamp(time);
                        caggEvaluationPublisher.publish(caggEvaluationMsg);
                    }
                    semanticTags.clear();
                }

                // spin rate in milliseconds
                Thread.sleep( SPINNING_RATE);
            }
        });
    }
}
