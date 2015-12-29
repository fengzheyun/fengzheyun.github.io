package retrieval;

import java.util.List;

/**
 * Extract the search results using Google in United States: www.google.com
 * Google API is applied.
 * Essentially, this is an API extract useful information from javascript, 
 * since Google API returns search results in the format of javascript
 * 
 * @author Zheyun Feng - fengzhey@msu.edu
 */
public class GoogleResult {

    private ResponseData responseData;
    private String responseDetails;
    private String responseStatus;
    public ResponseData getResponseData() { return responseData; }
    public String getResponseDetail() { return responseDetails; }
    public String getResponseStatus() { return responseStatus; }
    public void setResponseData(ResponseData responseData) { this.responseData = responseData; }
    public String toString() { return "ResponseData[" + responseData + "]"; }

    /*
     * define the return structure, and then translate the required elements from javascript, 
     * and then fill these elements in the previously defined structure
     */
    static class ResponseData {
        private List<Result> results;
        public List<Result> getResults() { return results; }
        public void setResults(List<Result> results) { this.results = results; }
        public String toString() { return "Results[" + results + "]"; }
    }

    /*
     * Subtree of the defined structure
     */
    static class Result {
        private String url; // url of returned document
        private String title; // title of the document
        private String content;  // the short description appears in the Google's returned webpage
        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public void setUrl(String url) { this.url = url; }
        public void setTitle(String title) { this.title = title; }
        public String toString() { return "Result[url:" + url +",title:" + title + "]"; }
    }

}