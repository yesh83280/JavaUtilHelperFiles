import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.learning.Uninstaller.beans.EmpSoftwareDtlsDTO;
import com.learning.Uninstaller.beans.InputDataDTO;
import com.learning.Uninstaller.beans.PredictionScoreDTO;
import com.learning.Uninstaller.constants.GSConstants;
import com.learning.Uninstaller.util.TrackExecutionTime;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GetDataUsingRestTemplate implements IDeployedModelBO, GSConstants {

	final String API_KEY = "";
	final String ACCESS_TOKEN_FETCH_URL = "";
	String iamToken = null;
	LocalDateTime iamExpiry = LocalDateTime.now();

	@Override
	@TrackExecutionTime
	public JsonNode fetchResponseFromMLModel(final String mlModelUrl, List<InputDataDTO> inputList) {

		JsonNode response = null;

		// To get Access Token/ Bearer Token with API_KEY
		LocalDateTime now = LocalDateTime.now();
		boolean isIamExpired = now.isAfter(iamExpiry);
		if(iamToken == null || isIamExpired) {
			getAccessToken(ACCESS_TOKEN_FETCH_URL, API_KEY);
		}

		if (iamToken != null && !iamToken.isEmpty()) {
			response = getResponseFromDeployedModel(mlModelUrl, iamToken, inputList);
		} else {
			iamExpiry = LocalDateTime.now();
		}

		return response;

	}

	@Override
	public List<InputDataDTO> snippetAsInputDataForWatson(List<EmpSoftwareDtlsDTO> softwareList) {

		List<InputDataDTO> inputList = new ArrayList<>();

//		Creating Input Data
		InputDataDTO inputData = new InputDataDTO();

//		Populating Field values
		List<String> fields = new ArrayList<>(Arrays.asList(AI_MODEL_FIELDS_LIST));
		inputData.setFields(fields);

		List<List<String>> valuesList = new ArrayList<>();

		softwareList.forEach(software -> {

			List<String> values = new ArrayList<>();

			values.add(null);
			values.add(null);
			values.add(null);
			values.add(null);
			values.add(null);
			values.add(null);
			values.add(null);
			values.add(null);
			values.add(software.getSnippet());
			values.add(null);
			values.add(null);

			valuesList.add(values);
		});

		inputData.setValues(valuesList);

		inputList.add(inputData);

		return inputList;
	}

	@Override
	public List<PredictionScoreDTO> modifyingOutputFromWatson(
			JsonNode response,
			String predictColName){
		List<PredictionScoreDTO> predScoreList = new ArrayList<>();

		ArrayNode responseFieldsList = (ArrayNode) response.get("predictions").get(0).get("fields");
		ArrayNode responseValuesList = (ArrayNode) response.get("predictions").get(0).get("values");

		for (int j = 0; j < responseValuesList.size(); j++) {
			PredictionScoreDTO predScoreDto = new PredictionScoreDTO();

			JsonNode value = responseValuesList.get(j);
			HashMap<String, Double> map = new HashMap<>();
			for (int i = 0; i < value.size(); i++) {
				String key = responseFieldsList.get(i).asText();
				if (key.startsWith("$SP-") && !key.endsWith(predictColName)) {
					String newKey = key.replace("$SP-", "");
					boolean isDouble = value.get(i).isDouble();
					if (isDouble) {
						Double val = value.get(i).asDouble();
						map.put(newKey, val);
					}
				}

				// Predicted Label assignment
				if (key.startsWith("$S-")) {
					String newKey = key.replace("$S-", "");

					// Passing index and Predicted Label Value
					predScoreDto.setLabel(value.get(i).asText());
				}

				// Predicted Score assignment
				if (key.endsWith(predictColName)) {
					boolean isDouble = value.get(i).isDouble();
					if (isDouble) {
						Double val = value.get(i).asDouble();

						// Passing index and Predicted Score
						predScoreDto.setScore(val);
					}
				}

			}
			predScoreDto.setMap(map);
			predScoreList.add(predScoreDto);
		}

		return predScoreList;
	}

	@Override
	public String getAccessToken(String connectionUrl, String API_Key) {

		String accessToken = "";

		try {

//			 Create Rest API Call
			RestTemplate restTemplate = new RestTemplate();

			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			map.add("apikey", API_Key);
			map.add("grant_type", "urn:tmp:params:oauth:grant-type:apikey");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

			// Getting response from Rest API Call
			ResponseEntity<String> response = restTemplate.exchange(connectionUrl, HttpMethod.POST, request,
					String.class);

			// Parse response JSON body for active status
			if (response.getStatusCode() == HttpStatus.OK) {
				ObjectMapper mapper = new ObjectMapper();

//				System.out.println("Response: "+response.getBody());
				JsonNode root = mapper.readTree(response.getBody());
				accessToken = root.path("access_token").asText();
				int expiresIn = root.path("expires_in").asInt();

				iamToken = accessToken;
				iamExpiry = LocalDateTime.now().plusSeconds(expiresIn).minusMinutes(10);

			}
		} catch (Exception e) {
			System.out.println("Failed to get Access Token " + e.getMessage());
			return null;
		}

		return accessToken;

	}

	@Override
	public JsonNode getResponseFromDeployedModel(String connectionUrl, String bearerToken,
			List<InputDataDTO> inputList) {

		JsonNode outputData = null;

		try {

//			 Create Rest API Call
			RestTemplate restTemplate = new RestTemplate();

			Map<String, List<InputDataDTO>> map = new HashMap<>();
			map.put("input_data", inputList);

//			System.out.println("Input Data" + map);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.add("Authorization", "Bearer " + bearerToken);

//			System.out.println("Header" + headers);

			HttpEntity<Map<String, List<InputDataDTO>>> request = new HttpEntity<>(map, headers);

			// Getting response from Rest API Call
			ResponseEntity<String> response = restTemplate.exchange(connectionUrl, HttpMethod.POST, request,
					String.class);

			// Parse response JSON body for active status
			if (response.getStatusCode() == HttpStatus.OK) {
				ObjectMapper mapper = new ObjectMapper();

//				System.out.println("Response: " + response.getBody());
				outputData = mapper.readTree(response.getBody());
//				accessToken = root.path("access_token").toString();
			}
		} catch (Exception e) {
			System.out.println("Failed to get Response - AI Model is not responding " + e.getMessage());
			return null;
		}

		return outputData;
	}
}
