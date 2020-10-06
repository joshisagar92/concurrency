package ch1_thread_safety;

import javax.servlet.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Locking {

    public static void main(String[] args) {

    }

}

class UnsafeLockingFactorizer implements Servlet{

    AtomicReference<BigInteger> lastNumber = new AtomicReference<>();
    AtomicReference<BigInteger []> lastFactor = new AtomicReference<>();

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
       BigInteger i =  extractFromRequest(servletRequest);

       if(i.equals(lastNumber.get())){
           encodeIntToResponse(servletResponse,lastFactor.get());
       }else {
           BigInteger[] factor = factor(i);
           lastNumber.set(i);
           lastFactor.set(factor);
           encodeIntToResponse(servletResponse,factor);
       }

    }

    private BigInteger[] factor(BigInteger i) {
            return new BigInteger[0];
    }

    private void encodeIntToResponse(ServletResponse servletResponse, BigInteger[] bigIntegers) {

    }

    private BigInteger extractFromRequest(ServletRequest servletRequest) {
            return null;
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
}


