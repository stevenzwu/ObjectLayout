package org.ObjectLayout;

import org.ObjectLayout.intrinsifiable.StructuredArrayIntrinsifiableModelBase;

/**
 * A model that describes the structure of a StructuredArray
 *
 * @param <S> The class of the StructuredArray modeled by the model
 * @param <T> The class of the elements in the StructuredArray modeled by the model
 */
public class StructuredArrayModel<S extends StructuredArray<T>, T> extends
        StructuredArrayIntrinsifiableModelBase<S, T> {

    /**
     * Create a model of a StructuredArray instance with terminal (non StructuredArray) elements
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param elementClass The class of the elements in the StructuredArray modeled by the model
     * @param length The length of the StructuredArray modeled by the model
     */
    public StructuredArrayModel(final Class<S> arrayClass,
                                final Class<T> elementClass,
                                final long length) {
        super(arrayClass, elementClass, length);
    }

    /**
     * Create a model of a StructuredArray instance with elements that are themselves StructuredArrays
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param subArrayModel The model describing the structure of the elements of the array being modeled
     * @param length The length of the StructuredArray modeled by the model
     * @param <A2> The class of the StructuredArray modeled by the subArrayModel
     * @param <T2> The class of the elements in the StructuredArray modeled by the subArrayModel
     */
    @SuppressWarnings("unchecked")
    public <A2 extends StructuredArray<T2>, T2>
    StructuredArrayModel(final Class<S> arrayClass,
                         final StructuredArrayModel<A2, T2> subArrayModel,
                         final long length) {
        super(arrayClass, subArrayModel, length);
    }

    /**
     * Determine if this model is equal to another object. If the other object is not a model, they are
     * not equal. If the other object is a model, the two are equal if all details, (arrayClass, elementClass,
     * length, and any subArrayModel hierarchy details) are identical.
     *
     * @param other the other object
     * @return true is the other object is a model that is equal to this one, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StructuredArrayIntrinsifiableModelBase)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        StructuredArrayModel<S, T> otherArray = (StructuredArrayModel<S, T>) other;
        if ((getArrayClass() != otherArray.getArrayClass()) ||
                (getElementClass() != otherArray.getElementClass()) ||
                (getLength() != otherArray.getLength())) {
            return false;
        }
        if ((getSubArrayModel() == null) && (otherArray.getSubArrayModel() == null)) {
            return true;
        }
        // if either subArrayModel is null, they are not equal. Otherwise, compare subArrays:
        return (getSubArrayModel() != null) &&
                (otherArray.getSubArrayModel() != null) &&
                getSubArrayModel().equals(otherArray.getSubArrayModel());
    }

    /**
     * Get the class of the StructuredArray modeled by this model
     * @return the class of the StructuredArray modeled by this model
     */
    public final Class<S> getArrayClass() {
        return super._getArrayClass();
    }

    /**
     * Get the class of the elements in the StructuredArray modeled by the model
     * @return the class of the elements in the StructuredArray modeled by the model
     */
    public final  Class<T> getElementClass() {
        return super._getElementClass();
    }

    /**
     * Get the model describing the structure of the elements of the array being modeled
     * @return the model describing the structure of the elements of the array being modeled
     */
    public final  StructuredArrayModel getSubArrayModel() {
        return (StructuredArrayModel) super._getSubArrayModel();
    }

    /**
     * Get the length of the StructuredArray modeled by the model
     * @return The length of the StructuredArray modeled by the model
     */
    public final  long getLength() {
        return super._getLength();
    }
}
