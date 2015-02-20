package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.HealthPortUserInfo;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;
import edu.gatech.i3l.HealthPort.ports.SyntheticEHRPort;

public class ObservationResourceProvider implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT U1.ID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)";

	private SyntheticEHRPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;
	private HealthPortUserInfo healthPortUser;
	private SyntheticEHRPort syntheticCancerPort;

	// Constructor
	public ObservationResourceProvider() {
		syntheticEHRPort = new SyntheticEHRPort("jdbc/ExactDataSample", HealthPortUserInfo.SyntheticEHR);
		healthvaultPort = new HealthVaultPort();
		healthPortUser = new HealthPortUserInfo("jdbc/HealthPort");
		syntheticCancerPort = new SyntheticEHRPort("jdbc/ExactDataCancer", HealthPortUserInfo.SyntheticCancer);
	}

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	public Class<Observation> getResourceType() {
		return Observation.class; 
	}

	@Read()
	public Observation getResourceById(@IdParam IdDt theId) {
		Observation obs = null;
		String resourceId = theId.getIdPart();
		//System.out.println(resourceId);
		String[] Ids = resourceId.split("\\-", 3);

//		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(
//				Integer.parseInt(Ids[0]));
		healthPortUser.setInformation(Ids[0]);
		String location = healthPortUser.source;
		
		if (location == null) return obs;

		//System.out.println(location);
		if (location.equals(HealthPortUserInfo.GREENWAY)) {
			System.out.println("Greenway");

		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			obs = syntheticEHRPort.getObservation(resourceId);

		} else if (location.equals(HealthPortUserInfo.HEALTHVAULT)) {
			obs = healthvaultPort.getObservation(resourceId);
		}else if (location.equals(HealthPortUserInfo.SyntheticCancer)) {
			obs = syntheticCancerPort.getObservation(resourceId);

		}

		return obs;
	}

	@Search
	public List<Observation> getAllObservations() {
		Connection connection = null;
		Statement statement = null;
		String ccd = null;

		ArrayList<Observation> finalRetVal = new ArrayList<Observation>();
		ArrayList<Observation> retVal = null;
		try {
			connection = healthPortUser.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
				healthPortUser.setRSInformation(resultSet);
				if (healthPortUser.source
						.equals(HealthPortUserInfo.GREENWAY)) {
					ccd = GreenwayPort.getCCD(healthPortUser.personId);
					// System.out.println(ccd);

				} else if (healthPortUser.source
						.equals(HealthPortUserInfo.SyntheticEHR)) {
					// retVal = new
					// SyntheticEHRPort().getObservations(HealthPortUser);
					// finalRetVal.addAll(retVal);
					retVal = syntheticEHRPort.getObservations(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				} else if (healthPortUser.source
						.equals(HealthPortUserInfo.HEALTHVAULT)) {
//					retVal = new HealthVaultPort()
//							.getObservations(healthPortUser);
					retVal = healthvaultPort.getObservations(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				} else if (healthPortUser.source
						.equals(HealthPortUserInfo.SyntheticCancer)) {
					retVal = syntheticCancerPort.getObservations(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}

				}

				retVal = null;
			}
			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return finalRetVal;

	}

	@Search()
	public List<Observation> getObservationsbyPatient(
			@RequiredParam(name = Observation.SP_SUBJECT) ReferenceParam theSubject) {
		String ccd = null;

		ArrayList<Observation> retVal = null;
		String PatientID;

		if (theSubject.hasResourceType()) {
			String resourceType = theSubject.getResourceType();
			if ("Patient".equals(resourceType) == false) {
				throw new InvalidRequestException(
						"Invalid resource type for parameter 'subject': "
								+ resourceType);
			} else {
				PatientID = theSubject.getIdPart();
			}
		} else {
			throw new InvalidRequestException(
					"Need resource type for parameter 'subject'");
		}

//		int patientNum = Integer.parseInt(PatientID);
//		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);

		healthPortUser.setInformation(PatientID);
		if (healthPortUser.source == null) return retVal;

		if (healthPortUser.source.equals(HealthPortUserInfo.GREENWAY)) {
			ccd = GreenwayPort.getCCD(healthPortUser.personId);
			// System.out.println(ccd);
		} else if (healthPortUser.source
				.equals(HealthPortUserInfo.SyntheticEHR)) {
			retVal = syntheticEHRPort.getObservations(healthPortUser);
		} else if (healthPortUser.source
				.equals(HealthPortUserInfo.HEALTHVAULT)) {
			retVal = healthvaultPort.getObservations(healthPortUser);
		} else if (healthPortUser.source
				.equals(HealthPortUserInfo.SyntheticCancer)) {
			retVal = syntheticCancerPort.getObservations(healthPortUser);

		}

		return retVal;

	}

	@Search()
	public List<Observation> searchByName(
			@RequiredParam(name = Observation.SP_NAME) TokenParam theName) {
		// This is search by Observation.name - codeableconcept.
		// Observation in FHIR supports LOINC. And, the data we have in the
		// Synthetic EHR has only LOINC in observation data.
		String systemName = theName.getSystem();
		String codeName = theName.getValue();
		// System.out.println(identifierSystem);
		//System.out.println(systemName + ":" + codeName);
		
		List<Observation> retVal = new ArrayList<Observation>();
		List<Observation> portRet = null;
		
		portRet = syntheticEHRPort.getObservationsByCodeSystem(systemName,
				codeName);
		if (portRet != null && !portRet.isEmpty()) {
			retVal.addAll(portRet);
		}
		
		portRet = syntheticCancerPort.getObservationsByCodeSystem(systemName,
				codeName);
		if (portRet != null && !portRet.isEmpty()) {
			retVal.addAll(portRet);
		}
		
		return retVal;
	}

}