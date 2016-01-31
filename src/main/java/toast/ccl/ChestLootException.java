package toast.ccl;

public class ChestLootException extends RuntimeException
{
    public ChestLootException(String comment, String path) {
        super(comment + " at " + path);
    }
    
    public ChestLootException(String comment, String path, Exception ex) {
        super(comment + " at " + path, ex);
    }
}