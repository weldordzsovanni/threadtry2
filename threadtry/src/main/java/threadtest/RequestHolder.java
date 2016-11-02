package threadtest;

import java.util.Objects;

/**
 * Created by NA on 2016.10.29..
 */
public class RequestHolder {

    private String payload;
        private Integer retryCount=0;

    public void incrementRetry(){
        retryCount++;
    }
    public RequestHolder(String payload) {
        this.payload = payload;
    }

    public RequestHolder(String payload, Integer retryCount) {
        this.payload = payload;
        this.retryCount = retryCount;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }


    public Integer getRetryCount() {
        return retryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestHolder that = (RequestHolder) o;
        return Objects.equals(payload, that.payload) &&
                Objects.equals(retryCount, that.retryCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payload, retryCount);
    }

    @Override
    public String toString() {
        return "RequestHolder{" +
                "payload='" + payload + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}
