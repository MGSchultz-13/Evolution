package tgw.evolution.util.collection.maps;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import tgw.evolution.util.collection.lists.IArrayList;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

public class I2FHashMap extends Int2FloatOpenHashMap implements I2FMap {

    protected @Nullable View view;
    protected @Nullable IArrayList wrappedEntries;

    @Override
    public long beginIteration() {
        if (this.wrappedEntries != null) {
            this.wrappedEntries.clear();
        }
        if (this.isEmpty()) {
            return 0;
        }
        if (this.containsNullKey) {
            return (long) this.n << 32 | this.size;
        }
        for (int pos = this.n; pos-- != 0; ) {
            int k = this.key[pos];
            if (k != 0) {
                return (long) pos << 32 | this.size;
            }
        }
        throw new IllegalStateException("Should never reach here");
    }

    @Override
    public int getIterationKey(long it) {
        int pos = (int) (it >> 32);
        if (pos >= 0) {
            return this.key[pos];
        }
        assert this.wrappedEntries != null;
        return this.wrappedEntries.get(-pos - 1);
    }

    @Override
    public float getIterationValue(long it) {
        int pos = (int) (it >> 32);
        if (pos >= 0) {
            return this.value[pos];
        }
        assert this.wrappedEntries != null;
        int k = this.wrappedEntries.get(-pos - 1);
        int p = HashCommon.mix(k) & this.mask;
        while (k != this.key[p]) {
            p = p + 1 & this.mask;
        }
        return this.value[p];
    }

    @Override
    public int getSampleKey() {
        if (this.isEmpty()) {
            throw new NoSuchElementException("Empty set");
        }
        if (this.containsNullKey) {
            return this.key[this.n];
        }
        for (int pos = this.n; pos-- != 0; ) {
            int k = this.key[pos];
            if (k != 0) {
                return k;
            }
        }
        throw new IllegalStateException("Should never reach here");
    }

    @Override
    public float getSampleValue() {
        if (this.isEmpty()) {
            throw new NoSuchElementException("Empty set");
        }
        if (this.containsNullKey) {
            return this.value[this.n];
        }
        for (int pos = this.n; pos-- != 0; ) {
            int k = this.key[pos];
            if (k != 0) {
                return this.value[pos];
            }
        }
        throw new IllegalStateException("Should never reach here");
    }

    @Override
    public FastEntrySet int2FloatEntrySet() {
        this.deprecatedMethod();
        return super.int2FloatEntrySet();
    }

    @Override
    public IntSet keySet() {
        this.deprecatedMethod();
        return super.keySet();
    }

    @Override
    public long nextEntry(long it) {
        if (this.isEmpty()) {
            return 0;
        }
        int size = (int) it;
        if (--size == 0) {
            return 0;
        }
        int pos = (int) (it >> 32);
        final int[] key = this.key;
        while (true) {
            if (--pos < 0) {
                return (long) pos << 32 | size;
            }
            if (key[pos] != 0) {
                return (long) pos << 32 | size;
            }
        }
    }

    @Override
    public long removeIteration(long it) {
        int pos = (int) (it >> 32);
        if (pos == this.n) {
            this.containsNullKey = false;
            this.key[this.n] = 0;
            this.value[this.n] = 0;
        }
        else if (pos >= 0) {
            this.iterationShiftKeys(pos);
        }
        else {
            assert this.wrappedEntries != null;
            int wrappedEntry;
            try {
                wrappedEntry = this.wrappedEntries.getInt(-pos - 1);
            }
            catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException(e);
            }
            this.remove(wrappedEntry);
            return it;
        }
        --this.size;
        return it;
    }

    @Override
    public FloatCollection values() {
        this.deprecatedMethod();
        return super.values();
    }

    @Override
    public @UnmodifiableView I2FMap view() {
        if (this.view == null) {
            this.view = new View(this);
        }
        return this.view;
    }

    protected void iterationShiftKeys(int pos) {
        final int[] key = this.key;
        while (true) {
            int last = pos;
            pos = pos + 1 & this.mask;
            int curr;
            while (true) {
                if ((curr = key[pos]) == 0) {
                    key[last] = 0;
                    this.value[last] = 0;
                    return;
                }
                int slot = HashCommon.mix(curr) & this.mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) {
                    break;
                }
                pos = pos + 1 & this.mask;
            }
            if (pos < last) {
                if (this.wrappedEntries == null) {
                    this.wrappedEntries = new IArrayList(2);
                }
                this.wrappedEntries.add(key[pos]);
            }
            key[last] = curr;
            this.value[last] = this.value[pos];
        }
    }
}
