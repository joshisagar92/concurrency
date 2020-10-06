package ch1_thread_safety;

import javax.servlet.*;
import java.io.IOException;
import java.math.BigInteger;

public class Complete implements Servlet {
    BigInteger lastNumber;
    BigInteger[] lastFactor;
    private long hits;
    private long cacheHits;

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        BigInteger i = extractFromRequest(servletRequest);
        BigInteger[] factor = null;
        synchronized (this) {
            hits++;
            if (i.equals(lastNumber)) {
                factor = lastFactor.clone();
                cacheHits++;
            }
        }
        if (factor == null) {
            factor = factor(i);
            synchronized (this) {
                lastNumber = i;
                lastFactor = factor.clone();
            }
        }
        encodeIntToResponse(servletResponse, factor);
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }


    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }

    private BigInteger[] factor(BigInteger i) {
        return new BigInteger[0];
    }

    private void encodeIntToResponse(ServletResponse servletResponse, BigInteger[] bigIntegers) {

    }

    private BigInteger extractFromRequest(ServletRequest servletRequest) {
        return null;
    }
}