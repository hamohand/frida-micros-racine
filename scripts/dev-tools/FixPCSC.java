import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class FixPCSC {
    public static void fix() {
        try {
            Class<?> pcscClass = Class.forName("sun.security.smartcardio.PCSCTerminals");
            Field contextIdField = pcscClass.getDeclaredField("contextId");
            
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            
            long fieldOffset = unsafe.staticFieldOffset(contextIdField);
            unsafe.putLong(unsafe.staticFieldBase(contextIdField), fieldOffset, 0L);
            System.out.println("PC/SC Context ID cleared successfully via Unsafe!");
        } catch (Throwable t) {
            System.out.println("Could not clear PC/SC context: " + t.getMessage());
            t.printStackTrace();
        }
    }
    public static void main(String[] args) {
        fix();
    }
}
