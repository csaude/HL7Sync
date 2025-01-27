package mz.org.csaude.hl7sync.generator;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v251.message.ADT_A24;
import mz.org.csaude.hl7sync.model.PatientDemographic;


import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author machabane
 */
public class AdtMessageFactory {

	static final Logger LOG = Logger.getLogger(AdtMessageFactory.class.getName());

	public static ADT_A24 createMessage(String messageType, PatientDemographic demographics)
	        throws HL7Exception, IOException {

		if (messageType.equals("A24")) {
			return new OurAdtA04MessageBuilder().Build(demographics);
		}

		throw new RuntimeException(String.format("%s message type is not supported yet. Extend this if you need to",
		    messageType));
	}
}
