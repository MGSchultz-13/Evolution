package tgw.evolution.client.models.data;

import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

public interface IModelData {

    IModelData EMPTY = new EmptyModelData();

    @Nullable <T> T getData(ModelProperty<T> prop);

    default long getLongData(ModelProperty<Long> prop) {
        Long data = this.getData(prop);
        return data == null ? 0L : data;
    }

    /**
     * Check if this data has a property, even if the value is {@code null}. Can be
     * used by code that intends to fill in data for a render pipeline, such as the
     * forge animation system.
     * <p>
     * IMPORTANT: {@link #getData(ModelProperty)} <em>can</em> return {@code null}
     * even if this method returns {@code true}.
     *
     * @param prop The property to check for inclusion in this model data
     * @return {@code true} if this data has the given property, even if no value is present
     */
    boolean hasProperty(ModelProperty<?> prop);

    <T> void setData(ModelProperty<T> prop, T data);

    default void setData(ModelProperty<Long> prop, long data) {
        this.setData(prop, Long.valueOf(data));
    }

    final class EmptyModelData implements IModelData {

        private EmptyModelData() {
        }

        @Override
        public @Nullable <T> T getData(ModelProperty<T> prop) {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasProperty(ModelProperty<?> prop) {
            return false;
        }

        @Override
        public <T> void setData(ModelProperty<T> prop, T data) {
            throw new NoSuchElementException();
        }
    }
}
