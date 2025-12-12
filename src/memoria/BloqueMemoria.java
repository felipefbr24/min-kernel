package memoria;

public class BloqueMemoria {
    public final int start;
    public final int size;
    public final Integer pid;

    public BloqueMemoria(int start, int size, Integer pid) {
        this.start = start;
        this.size = size;
        this.pid = pid;
    }

    public static BloqueMemoria fromBlock(MemoryBlock block) {
        return new BloqueMemoria(block.start, block.size, block.pid);
    }

    public boolean isFree() {
        return pid == null;
    }
}
