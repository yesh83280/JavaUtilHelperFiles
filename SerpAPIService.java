
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SerpAPIService {

    @Autowired
    SerpAnalysisDAO serpAnalysisDAO;

    @Value("${serp.api.key}")
    String serpAPIKey;

    public JsonNode fetchSerpResponseByQuestion(String question) {

        JsonNode jsonResponse = null;
        try {

            String connectionUrl = "https://serpapi.com/search.json?q={question}&hl={hl}&google_domain={domain}&api_key={secret_api_key}";

 //         Assigning parameters to SERP API call
            Map<String, String> parameters = new HashMap<>();
            parameters.put("question", question);
            parameters.put("hl", "en");
            parameters.put("domain", "google.com");
            parameters.put("secret_api_key", serpAPIKey);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(connectionUrl, HttpMethod.GET, request,
                    String.class, parameters);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                jsonResponse = mapper.readTree(response.getBody());
//				System.out.println("Json Node: "+root.toPrettyString());
            }

        } catch (Exception e) {
            System.out.println("Exception occured in fetching SerpResponse " + e.getMessage());
            return null;
        }

        return jsonResponse;

    }

    public List<EmpSoftwareDtlsDTO> analyzeGoogleResult(EmpSoftwareDtlsDTO empMachineDtlsDTO, String question, JsonNode googleResponse) {

        List<EmpSoftwareDtlsDTO> softwareGoogleRespDTOList = new ArrayList<>();

//      Fetching first two Organic Results from Google Response
        JsonNode organicResults = googleResponse.get("organic_results");
        if(organicResults!=null && organicResults.isArray()){
               List<JsonNode> organicResultsArray = StreamSupport.stream(organicResults.spliterator(), false).collect(Collectors.toList());

               if(organicResultsArray!=null){

                   for (int i = 0; i < organicResultsArray.size(); i++) {
                       if(i<=1){
                           JsonNode result = organicResultsArray.get(i);
                           if(result != null){
                               EmpSoftwareDtlsDTO softwareGoogleRespDTO = new EmpSoftwareDtlsDTO();

                               if(result.get("snippet")!=null){
                                   softwareGoogleRespDTO.setSnippet(result.get("snippet").asText());
                               }
                               if(result.get("link")!=null){
                                   softwareGoogleRespDTO.setReferenceUrl(result.get("link").asText());
                               }
                               softwareGoogleRespDTO.setCreatedTmsp(new Timestamp(System.currentTimeMillis()));

                               softwareGoogleRespDTOList.add(softwareGoogleRespDTO);
                           }
                       }
                   }
               }
           }

//      Fetching related questions and answers from Google response
        JsonNode relatedQuestions = googleResponse.get("related_questions");

        if(relatedQuestions!=null && relatedQuestions.isArray()){
            List<JsonNode> relatedQuestionsArray = StreamSupport.stream(relatedQuestions.spliterator(), false).collect(Collectors.toList());

            relatedQuestionsArray.forEach(value -> {
                EmpSoftwareDtlsDTO softwareGoogleRespDTO = new EmpSoftwareDtlsDTO();

                if(value.get("question")!=null){
                    softwareGoogleRespDTO.setRelatedQuestion(value.get("question").asText());
                }
                if(value.get("snippet")!=null){
                    softwareGoogleRespDTO.setSnippet(value.get("snippet").asText());
                }
                if(value.get("link")!=null){
                    softwareGoogleRespDTO.setReferenceUrl(value.get("link").asText());
                }
                softwareGoogleRespDTO.setCreatedTmsp(new Timestamp(System.currentTimeMillis()));

                softwareGoogleRespDTOList.add(softwareGoogleRespDTO);
            });

        }

//      Assigning emp machine dtls to software google response
        softwareGoogleRespDTOList.forEach(value -> {
            value.setSoftwareNm(empMachineDtlsDTO.getSoftwareNm());
            value.setModifiedSoftwareNm(empMachineDtlsDTO.getModifiedSoftwareNm());
            value.setSoftwareVersion(empMachineDtlsDTO.getSoftwareVersion());
            value.setSoftwarePublisher(empMachineDtlsDTO.getSoftwarePublisher());
            value.setLicenseType(empMachineDtlsDTO.getLicenseType());
            value.setLicenseWikiUrl(empMachineDtlsDTO.getLicenseWikiUrl());
            value.setLicenseWikiSoftwareNm(empMachineDtlsDTO.getLicenseWikiSoftwareNm());
            value.setQuery(question);
        });

        return softwareGoogleRespDTOList;

    }

    public List<EmpSoftwareDtlsDTO> performSERPCallAndAnalyzeBySWName(EmpSoftwareDtlsDTO empSoftwareDtlsDTO) {

        List<EmpSoftwareDtlsDTO> softwareGoogleRespDTOList = new ArrayList<>();

        Arrays.stream(QueryConstants.questions).forEach(question -> {

            String query = question.replace(":software_name", empSoftwareDtlsDTO.getModifiedSoftwareNm());
//            System.out.println("Query: "+ query);
            JsonNode googleResponse = fetchSerpResponseByQuestion(query);
//            System.out.println("googleResponse = " + googleResponse);
            if(googleResponse!=null){
                softwareGoogleRespDTOList.addAll(analyzeGoogleResult(empSoftwareDtlsDTO, query, googleResponse));
            }
        });

        return softwareGoogleRespDTOList;
    }

    //Starting Serp Analysis Dao queries
    public List<EmpSoftwareDtlsDTO> fetchDistinctSwForSerpAnalysisISI(){
        return serpAnalysisDAO.fetchDistinctSwForSerpAnalysisISI();
    }

    public int[] insertInToSerpAnalysis(List<EmpSoftwareDtlsDTO> empSoftwareDtlsDTOList){
        return serpAnalysisDAO.insertInToSerpAnalysis(empSoftwareDtlsDTOList);
    }

    //Performing Serp Analysis By Software Name
    //Checks whether analysis is available - If available returns same data else Perform analysis and stores analysis
    public List<EmpSoftwareDtlsDTO> fetchSerpAnalysisBySwDetails(EmpSoftwareDtlsDTO softwareDtlsDTO) {

        //returning back if software name is null
        if(softwareDtlsDTO.getModifiedSoftwareNm()==null) {
            return new ArrayList<>();
        }

        List<EmpSoftwareDtlsDTO> existingSoftwaregoogleAnalysis = serpAnalysisDAO.fetchSerpAnalysisBySwName(softwareDtlsDTO.getModifiedSoftwareNm());

        //If google scrapping data is available already, showing same data
        if(existingSoftwaregoogleAnalysis!=null && existingSoftwaregoogleAnalysis.size()>0){
            return existingSoftwaregoogleAnalysis;
        }

        List<EmpSoftwareDtlsDTO> softwareGoogleRespDTOList = performSERPCallAndAnalyzeBySWName(softwareDtlsDTO);

        //Removing duplicate snippets in google response
        Set<String> snippetSet = new HashSet<>();
        softwareGoogleRespDTOList = softwareGoogleRespDTOList.stream().filter(val -> snippetSet.add(val.getSnippet())).collect(Collectors.toList());

        //Inserting google scrapping data after completing
        int[] insertStatus = serpAnalysisDAO.insertInToSerpAnalysis(softwareGoogleRespDTOList);
        if (Arrays.stream(insertStatus).allMatch(status -> status > 0)) {
            System.out.println("Inserted SERP Analysis successfully for " + softwareDtlsDTO.getSoftwareNm() + " software");
        } else {
            System.out.println("Something happened while inserting SERP Analysis of softwares");
        }

        return softwareGoogleRespDTOList;
    }

}
