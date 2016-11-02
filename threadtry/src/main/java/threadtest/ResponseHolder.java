package threadtest;

/**
 * Created by ZGY on 2016.10.29..
 */
public class ResponseHolder {

    private RequestHolder requestHolder;
    private String response;

    public ResponseHolder(RequestHolder requestHolder, String response) {
        this.requestHolder = requestHolder;
        this.response = response;
    }

    public RequestHolder getRequestHolder() {
        return requestHolder;
    }

    public void setRequestHolder(RequestHolder requestHolder) {
        this.requestHolder = requestHolder;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "ResponseHolder{" +
                "requestHolder=" + requestHolder +
                ", response='" + response + '\'' +
                '}';
    }
}
