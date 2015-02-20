package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Medication;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.HealthPortUserInfo;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;
import edu.gatech.i3l.HealthPort.ports.SyntheticEHRPort;

public class MedicationPrescrResource implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT U1.ID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)";

	private SyntheticEHRPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;
	private HealthPortUserInfo healthPortUser;
	private SyntheticEHRPort syntheticCancerPort;

	// Constructor
	public MedicationPrescrResource() {
		syntheticEHRPort = new SyntheticEHRPort("jdbc/ExactDataSample", HealthPortUserInfo.SyntheticEHR);
		healthvaultPort = new HealthVaultPort();
		healthPortUser = new HealthPortUserInfo("jdbc/HealthPort"); 
		syntheticCancerPort = new SyntheticEHRPort("jdbc/ExactDataCancer", HealthPortUserInfo.SyntheticCancer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return MedicationPrescription.class;
	}

	@Read()
	public MedicationPrescription getResourceById(@IdParam IdDt theId) {
		MedicationPrescription med = null;
		String resourceId = theId.getIdPart();
		String[] Ids = theId.getIdPart().split("\\-", 3);

		healthPortUser.setInformation(Ids[0]);
		String location = healthPortUser.source;

		if (location == null) return med;
		
		if (location.equals(HealthPortUserInfo.GREENWAY)) {
			System.out.println("Greenway");

		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			// med = new
			// SyntheticEHRPort().getMedicationPrescription(resourceId);
			med = syntheticEHRPort.getMedicationPrescription(resourceId);

		} else if (location.equals(HealthPortUserInfo.HEALTHVAULT)) {
			// med = new
			// HealthVaultPort().getMedicationPrescription(resourceId);
			med = healthvaultPort.getMedicationPrescription(resourceId);
		} else if (location.equals(HealthPortUserInfo.SyntheticCancer)) {
			med = syntheticCancerPort.getMedicationPrescription(resourceId);

		}

		return med;
	}

	@Search
	public List<MedicationPrescription> getAllMedicationPrescriptions() {
		Connection connection = null;
		Statement statement = null;
		String ccd = null;

		ArrayList<MedicationPrescription> finalRetVal = new ArrayList<MedicationPrescription>();
		ArrayList<MedicationPrescription> retVal = null;
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
					retVal = syntheticEHRPort
							.getMedicationPrescriptions(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				} else if (healthPortUser.source
						.equals(HealthPortUserInfo.HEALTHVAULT)) {
					retVal = healthvaultPort
							.getMedicationPrescriptions(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				} else if (healthPortUser.source
						.equals(HealthPortUserInfo.SyntheticCancer)) {
					retVal = syntheticCancerPort
							.getMedicationPrescriptions(healthPortUser);
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
	public List<MedicationPrescription> getMedicationPrescriptionsByPatient(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {

		ArrayList<MedicationPrescription> retVal = null;
		String ccd = null;
//		int patientNum = Integer.parseInt(theSubject.getIdPart());
//		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);
		healthPortUser.setInformation(theSubject.getIdPart());
		
		if (healthPortUser.source == null) return retVal;
		
		if (healthPortUser.source.equals(HealthPortUserInfo.GREENWAY)) {
			ccd = GreenwayPort.getCCD(healthPortUser.personId);
			// System.out.println(ccd);
		} else if (healthPortUser.source
				.equals(HealthPortUserInfo.SyntheticEHR)) {
			// retVal = new
			// SyntheticEHRPort().getMedicationPrescriptions(HealthPortUser);
			retVal = syntheticEHRPort
					.getMedicationPrescriptions(healthPortUser);

		} else if (healthPortUser.source
				.equals(HealthPortUserInfo.HEALTHVAULT)) {
			// retVal = new
			// HealthVaultPort().getMedicationPrescriptions(HealthPortUser);
			retVal = healthvaultPort.getMedicationPrescriptions(healthPortUser);
		} else if (healthPortUser.source
				.equals(HealthPortUserInfo.SyntheticCancer)) {
			retVal = syntheticCancerPort
					.getMedicationPrescriptions(healthPortUser);

		}

		return retVal;
	}

	@Search
	public List<MedicationPrescription> findMedPrescriptListWithChain(
			@RequiredParam(name = MedicationPrescription.SP_MEDICATION, chainWhitelist = { Medication.SP_NAME }) ReferenceParam theMedication) {
		// This is /MedicationPrescription?medication.name=<medicine_name>.
		// We are chaining "name" parameter in Medication resource to MedicationPrescription.
		
		List<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
		List<MedicationPrescription> portRet = null;
		
		// The search parameter is medication in the MedicationPrescription resource
		// with chained search parameter, "name" in Medication resource. 
		// Check if the chained parameter search is "medication.name".
		String chain = theMedication.getChain();
		if (Medication.SP_NAME.equals(chain)) {
			String medName = theMedication.getValue();
			portRet = syntheticEHRPort.getMedicationPrescriptionsByType(medName);
			if (portRet != null && !portRet.isEmpty()) {
				retVal.addAll(portRet);
			}
			
			portRet = syntheticCancerPort.getMedicationPrescriptionsByType(medName);
			if (portRet != null && !portRet.isEmpty()) {
				retVal.addAll(portRet);
			}
		}

		return retVal;
	}

}