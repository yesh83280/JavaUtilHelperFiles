import org.jsoup.Connection.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JsoupWebScraping {

    String wikiBaseUrl = "https://en.wikipedia.org";
    String wikiSearchPath = "/w/index.php";

    String googleBaseUrl = "https://www.google.com";
    String googleSearchPath = "/search";

    public Document sendJsoupRequestByUrl(String url, Map<String, String> params){

        Document document = null;
        Response response = null;
        try{
            if(params!=null){
                response = Jsoup.connect(url).data(params).execute();
            }
            else {
                response = Jsoup.connect(url).execute();
            }
            if(response!= null && response.statusCode()==200){
                document = response.parse();
            }
        }catch(Exception e){
            System.out.println("Exception occured in sendJsoupRequestByUrl = " + e.getMessage());
            return null;
        }

        return document;
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public EmpSoftwareDtlsDTO getLicenseTypeBySoftwareNm(EmpSoftwareDtlsDTO empSoftwareDtlsDTO){

        Map<String, String> parameters = new HashMap<>();
                parameters.put("search", empSoftwareDtlsDTO.getModifiedSoftwareNm());
        parameters.put("title", "Special:Search");
        parameters.put("profile", "advanced");
        parameters.put("fulltext", "1");
        parameters.put("ns0","1");

        Document wikipediaDoc = sendJsoupRequestByUrl(wikiBaseUrl+ wikiSearchPath, parameters);

        if(wikipediaDoc!=null){
            Elements elements = wikipediaDoc.select("li.mw-search-result");
//          Checking for license types in top 3 results of WIKI Search
            for (int i = 0; i < elements.size(); i++) {
                if(i<3){
                    String wikiPageUrl = null;
                    if(elements.get(i)!=null){
                        Element tempElement = elements.get(i).getElementsByClass("mw-search-result-heading").first();
                        if(tempElement!=null){
                            Element anchorTag = tempElement.select("a").first();
                            wikiPageUrl = anchorTag!=null ? anchorTag.attr("href"): null;
                        }
                        if(wikiPageUrl!=null){
//                          System.out.println("Wiki Page Url = " + wikiPageUrl);
                            empSoftwareDtlsDTO.setLicenseWikiUrl(wikiPageUrl);
                            getWikiLicenseInfoByUrl(empSoftwareDtlsDTO);
                            if(empSoftwareDtlsDTO.getLicense()!=null && !empSoftwareDtlsDTO.getLicense().trim().isEmpty())
                                return empSoftwareDtlsDTO;
//                          System.out.println("License Name = " + empSoftwareDtlsDTO.getLicense());
                        }
                    }
                }
            }
        }
        else{
            System.out.println("Unable to send Jsoup request");
        }

        System.out.println("Unable to find license type for software: "+ empSoftwareDtlsDTO.getModifiedSoftwareNm());
        empSoftwareDtlsDTO.setLicense("NOT FOUND");
        //Making back license wiki url as null in case license type not found
        empSoftwareDtlsDTO.setLicenseWikiUrl("");
        empSoftwareDtlsDTO.setLicenseWikiSoftwareNm("");

        return empSoftwareDtlsDTO;

    }

    public EmpSoftwareDtlsDTO getWikiLicenseInfoByUrl(EmpSoftwareDtlsDTO empSoftwareDtlsDTO){


        final Document wikiPageDoc = sendJsoupRequestByUrl(wikiBaseUrl+ empSoftwareDtlsDTO.getLicenseWikiUrl(), null);

        if(wikiPageDoc!=null){
            Element licenseHeadingElement = wikiPageDoc.getElementsByAttributeValue("href","/wiki/Software_license").first();

            Element licenseHeadingParent = licenseHeadingElement!=null ? licenseHeadingElement.parent() : null;

            if(licenseHeadingParent!=null){
                Element licenseElement = licenseHeadingParent.parent().select("td").first();
                if(licenseElement!=null){
                    List<String> tempLicenseTypes = new ArrayList<>();
                    licenseElement.select("a").forEach(software -> {
                        //Removing licenses with dummy numbers as like [6],[7]....
                        if(software.text()!=null && !software.text().contains("[") && !software.text().contains("]")){
                            tempLicenseTypes.add(software.text());
                        }
                    });
                    empSoftwareDtlsDTO.setLicense(String.join(",", tempLicenseTypes));
                    Element licenseWikiHeading = wikiPageDoc.getElementById("firstHeading");
                    empSoftwareDtlsDTO.setLicenseWikiSoftwareNm(licenseWikiHeading.text());
                    empSoftwareDtlsDTO.setLicenseWikiUrl(wikiBaseUrl+ empSoftwareDtlsDTO.getLicenseWikiUrl());
                    return empSoftwareDtlsDTO;
                }
            }
        }
        else{
            System.out.println("Unable to send Jsoup request");
        }

        //Making back license wiki url as null in case license type not found
        empSoftwareDtlsDTO.setLicenseWikiUrl("");
        empSoftwareDtlsDTO.setLicenseWikiSoftwareNm("");
//        System.out.println("Unable to find license type for software: "+ empSoftwareDtlsDTO.getModifiedSoftwareNm());

        return empSoftwareDtlsDTO;

    }

    public List<EmpSoftwareDtlsDTO> getSERPAnalysisByWebScraping(EmpSoftwareDtlsDTO softwareDtlsDTO) {

        List<EmpSoftwareDtlsDTO> serpGoogleResponse = new ArrayList<>();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("q", softwareDtlsDTO.getQuery());

        Document serpGoogleRespDoc = sendJsoupRequestByUrl(googleBaseUrl+ googleSearchPath, parameters);

        if(serpGoogleRespDoc!=null){

//            Fetching related questions from google response
            Elements relatedQuestionEles = serpGoogleRespDoc.select("div[jsname=Cpkphb]");
            if(relatedQuestionEles!=null){
                relatedQuestionEles.forEach(relatedQuestionEle -> {
                    EmpSoftwareDtlsDTO tempRelatedQuestion = new EmpSoftwareDtlsDTO();
                    Element tempQuesEle = relatedQuestionEle.select("div[jsname=jIA8B]").first();
                    Element tempUrlEle = relatedQuestionEle.select("a[href]").first();
                    if(tempQuesEle!=null && tempUrlEle!=null){
                        tempRelatedQuestion.setModifiedSoftwareNm(softwareDtlsDTO.getModifiedSoftwareNm());
                        tempRelatedQuestion.setQuery(softwareDtlsDTO.getQuery());
                        tempRelatedQuestion.setRelatedQuestion(tempQuesEle.text());
                        tempRelatedQuestion.setReferenceUrl(tempUrlEle.attr("href"));
                        getSnippetFromGoogleUrl(tempRelatedQuestion);
                        serpGoogleResponse.add(tempRelatedQuestion);
                    }
                });
            }

            Elements searchResults = serpGoogleRespDoc.getElementsByClass("g");

            for (int i = 0; i < searchResults.size(); i++) {
//                Fetching first two google searchResults
                if(i<=1){
                    EmpSoftwareDtlsDTO googleSearchDtls = new EmpSoftwareDtlsDTO();
                    Element tempReferenceUrlEle = searchResults.get(i).select("a[href]").first();
                    Element tempSearchValueEle = searchResults.get(i).getElementsByClass("VwiC3b yXK7lf MUxGbd yDYNvb lyLwlc lEBKkf").first();
                    if(tempReferenceUrlEle!=null || tempSearchValueEle!= null){
                        googleSearchDtls.setModifiedSoftwareNm(softwareDtlsDTO.getModifiedSoftwareNm());
                        googleSearchDtls.setQuery(softwareDtlsDTO.getQuery());
                        if(tempReferenceUrlEle!=null){
                            googleSearchDtls.setReferenceUrl(tempReferenceUrlEle.attr("href"));
                        }
                        if(tempSearchValueEle!=null){
                            googleSearchDtls.setSnippet(tempSearchValueEle.text());
                        }
                        serpGoogleResponse.add(googleSearchDtls);
                    }
                }
                else {
                    break;
                }
            }

        }

//			System.out.println("Serp Google Response = " + serpGoogleResponse);
//        System.out.println("Query = " + question);
//        System.out.println("--------------------------");
//        for (EmpSoftwareDtlsDTO softwareDtlsDTO : serpGoogleResponse) {
//            System.out.println("--------------");
//            System.out.println("Related Question = " + softwareDtlsDTO.getRelatedQuestion());
//            System.out.println("Snippet = " + softwareDtlsDTO.getSnippet());
//            System.out.println("Reference Url = " + softwareDtlsDTO.getReferenceUrl());
//            System.out.println("--------------");
//        }
//        System.out.println("--------------------------");

        return serpGoogleResponse;

    }

    public EmpSoftwareDtlsDTO getSnippetFromGoogleUrl(EmpSoftwareDtlsDTO tempRelatedQuestion){

		String searchLink = tempRelatedQuestion.getReferenceUrl();

		if(searchLink.startsWith(googleSearchPath)){
			Document snippetDocument = sendJsoupRequestByUrl(googleBaseUrl+ searchLink, null);

			if(snippetDocument!=null){
				Element snippetElement = snippetDocument.getElementsByClass("g wF4fFd JnwWd g-blk").first();
				if(snippetElement!=null){
					Element innerTextEle = snippetElement.getElementsByClass("LGOjhe").first();
					Element tempUrlEle = snippetElement.select("a[href]").first();
					if(innerTextEle!=null){
						tempRelatedQuestion.setSnippet(innerTextEle.text());
					}
					if(tempUrlEle!=null){
						tempRelatedQuestion.setReferenceUrl(tempUrlEle.attr("href"));
					}
				}
			}

		}

		return tempRelatedQuestion;
	}

}
