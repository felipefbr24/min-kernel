package memoria;

import java.util.*;

public class GestorMemoria {
    private final List<MemoryBlock> blocks = new ArrayList<>();
    private final int totalSize;

    public GestorMemoria(int totalSize) {
        this.totalSize = totalSize;
        blocks.add(new MemoryBlock(0, totalSize, null));
    }

    public boolean allocate(int pid, int size) {
        for (int i = 0; i < blocks.size(); i++) {
            MemoryBlock block = blocks.get(i);
            if (block.isFree() && block.size >= size) {
                MemoryBlock used = new MemoryBlock(block.start, size, pid);
                if (block.size == size) {
                    blocks.set(i, used);
                } else {
                    block.start += size;
                    block.size -= size;
                    blocks.add(i, used);
                }
                return true;
            }
        }
        return false;
    }

    public void freeByPid(int pid) {
        for (MemoryBlock block : blocks) {
            if (Objects.equals(block.pid, pid)) {
                block.pid = null;
            }
        }
        mergeFreeBlocks();
    }

    private void mergeFreeBlocks() {
        Collections.sort(blocks, Comparator.comparingInt(b -> b.start));
        for (int i = 0; i < blocks.size() - 1; ) {
            MemoryBlock current = blocks.get(i);
            MemoryBlock next = blocks.get(i + 1);
            if (current.isFree() && next.isFree()) {
                current.size += next.size;
                blocks.remove(i + 1);
            } else {
                i++;
            }
        }
        if (blocks.isEmpty()) {
            blocks.add(new MemoryBlock(0, totalSize, null));
        }
    }

    public List<BloqueMemoria> getBlocks() {
        Collections.sort(blocks, Comparator.comparingInt(b -> b.start));
        List<BloqueMemoria> list = new ArrayList<>();
        for (MemoryBlock block : blocks) {
            list.add(BloqueMemoria.fromBlock(block));
        }
        return list;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public int getUsedSize() {
        int used = 0;
        for (MemoryBlock block : blocks) {
            if (!block.isFree()) {
                used += block.size;
            }
        }
        return used;
    }

    public int getFreeSize() {
        return totalSize - getUsedSize();
    }

    public void reset() {
        blocks.clear();
        blocks.add(new MemoryBlock(0, totalSize, null));
    }
}
