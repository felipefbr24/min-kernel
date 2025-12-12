package memoria;

class MemoryBlock {
    int start;
    int size;
    Integer pid; // null means free

    MemoryBlock(int start, int size, Integer pid) {
        this.start = start;
        this.size = size;
        this.pid = pid;
    }

    boolean isFree() {
        return pid == null;
    }
}
