package framework.telegram.support.tools.exception;


public class ClientException extends RuntimeException {
    private Throwable mException;
    private String mDetail;
    private int mCode;

    public ClientException() {
    }

    public ClientException(int code, String detail) {
        super(detail);
        this.setCode(code);
        this.setDetail(detail);
    }

    public ClientException(int code, Throwable ex) {
        this.setCode(code);
        this.setException(ex);
    }

    public ClientException(int code) {
        this.setCode(code);
    }

    public ClientException(Throwable ex) {
        this.setException(ex);
    }

    public ClientException(String detail) {
        super(detail);
        this.setDetail(detail);
    }

    public int getCode() {
        return this.mCode;
    }

    public void setCode(int code) {
        this.mCode = code;
    }

    public Throwable getException() {
        return this.mException;
    }

    public void setException(Throwable exception) {
        this.mException = exception;
    }

    public String getDetail() {
        return this.mDetail;
    }

    public void setDetail(String detail) {
        this.mDetail = detail;
    }
}
