import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.StringTokenizer;

public class InputReader implements AutoCloseable {

    private final BufferedReader reader;
    private StringTokenizer tokenizer;

    public InputReader(InputStream stream) {
        reader = new BufferedReader(new InputStreamReader(stream));
        tokenizer = null;
    }

    public String nextLine() throws IOException {
        return reader.readLine();
    }

    public String next() throws Exception {
        while (tokenizer == null || !tokenizer.hasMoreTokens()) {
            tokenizer = new StringTokenizer(nextLine());
        }
        return tokenizer.nextToken();
    }

    public Integer nextInt(int radix) {
        try {
            return Integer.parseInt(next(), radix);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer nextInt() {
        return nextInt(10);
    }

    public Double nextDouble() {
        try {
            return Double.parseDouble(next());
        } catch (Exception e) {
            return null;
        }
    }

    public BigInteger nextBigInteger(int radix) {
        try {
            return new BigInteger(next(), radix);
        } catch (Exception e) {
            return null;
        }
    }

    public BigInteger nextBigInteger() {
        return nextBigInteger(10);
    }

    public Long nextLong(int radix) {
        try {
            return Long.parseLong(next(), radix);
        } catch (Exception e) {
            return null;
        }
    }

    public Long nextLong() {
        return nextLong(10);
    }

    public String nextToken() {
        try {
            return next();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}