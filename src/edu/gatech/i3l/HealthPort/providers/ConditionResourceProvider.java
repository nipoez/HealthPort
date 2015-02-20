package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortUserInfo;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;
import edu.gatech.i3l.HealthPort.ports.SyntheticEHRPort;

public class ConditionResourceProvider implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT U1.ID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)";

	private SyntheticEHRPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;
	private HealthPortUserInfo healthPortUser;
	private SyntheticEHRPort syntheticCancerPort;

	// Constructor
	public ConditionResourceProvider() {
		syntheticEHRPort = new SyntheticEHRPort("jdbc/ExactDataSample", HealthPortUserInfo.SyntheticEHR);
		healthvaultPort = new HealthVaultPort();
		healthPortUser = new HealthPortUserInfo("jdbc/HealthPort");
		syntheticCancerPort = new SyntheticEHRPort("jdbc/ExactDataCancer", HealthPortUserInfo.SyntheticCancer);
	}

	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return Condition.class;
	}

	@Read()
	public Condition getResourceById(@IdParam IdDt theId) {
		Condition cond = null;
		String resourceId = theId.getIdPart();
		String[] Ids = theId.getIdPart().split("\\-", 3);

//		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(
//				Integer.parseInt(Ids[0]));
		healthPortUser.setInformation(Ids[0]);
		String location = healthPortUser.source;

		if (location.equals(HealthPortUserInfo.GREENWAY)) {
			System.out.println("Greenway");

		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			cond = syntheticEHRPort.getCondition(resourceId);
		} else if (location.equals(HealthPortUserInfo.HEALTHVAULT)) {
			cond = healthvaultPort.getCondition(resourceId);
		} else if (location.equals(HealthPortUserInfo.SyntheticCancer)) {
			cond = syntheticCancerPort.getCondition(resourceId);
		}

		return cond;
	}

	@Search
	public List<Condition> getAllConditions() {
		Connection connection = null;
		Statement statement = null;
		String ccd = null;

		ArrayList<Condition> finalRetVal = new ArrayList<Condition>();
		ArrayList<Condition> retVal = null;

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
					retVal = syntheticEHRPort.getConditions(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				} else if (healthPortUser.source
						.equals(HealthPortUserInfo.HEALTHVAULT)) {
					retVal = healthvaultPort.getConditions(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				} else if (healthPortUser.source
						.equals(HealthPortUserInfo.SyntheticCancer)) {
					retVal = syntheticCancerPort.getConditions(healthPortUser);
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
	public List<Condition> getConditionsByPatient(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {

		String location = null;
		String ccd = null;
//		int patientNum = Integer.parseInt(theSubject.getIdPart());

		ArrayList<Condition> retVal = null;

//		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);
		healthPortUser.setInformation(theSubject.getIdPart());
		String rId = healthPortUser.recordId;
		String pId = healthPortUser.personId;
		location = healthPortUser.source;

		if (location == null) return retVal;
		
		if (location.equals(HealthPortUserInfo.GREENWAY)) {
			ccd = GreenwayPort.getCCD(pId);
			// System.out.println(ccd);
		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			// retVal = new SyntheticEHRPort().getConditions(HealthPortUser);
			retVal = syntheticEHRPort.getConditions(healthPortUser);
		} else if (location.equals(HealthPortUserInfo.HEALTHVAULT)) {
			// retVal = new HealthVaultPort().getConditions(HealthPortUser);
			retVal = healthvaultPort.getConditions(healthPortUser);
		} else if (location.equals(HealthPortUserInfo.SyntheticCancer)) {
			// retVal = new SyntheticEHRPort().getConditions(HealthPortUser);
			retVal = syntheticCancerPort.getConditions(healthPortUser);
		}

		return retVal;
	}

	@Search()
	public List<Condition> searchByCode(
			@RequiredParam(name = Condition.SP_CODE) TokenParam theId) {
		String codeSystem = theId.getSystem();
		String code = theId.getValue();
		// System.out.println(codeSystem);
		// System.out.println(code);

		ArrayList<Condition> retVal = new ArrayList<Condition>();
		ArrayList<Condition> portRet = null;
		// retVal = new SyntheticEHRPort().getConditionsByCodeSystem(codeSystem,
		// code);
		// return retVal;
		portRet = syntheticEHRPort.getConditionsByCodeSystem(codeSystem, code);
		if (portRet != null && !portRet.isEmpty()) {
			retVal.addAll(portRet);
		}
		
		portRet = syntheticCancerPort.getConditionsByCodeSystem(codeSystem, code);
		if (portRet != null && !portRet.isEmpty()) {
			retVal.addAll(portRet);
		}
		
		return retVal;
	}

}