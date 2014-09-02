/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import org.ObjectLayout.intrinsifiable.StructuredArrayIntrinsifiableBase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.lang.reflect.Modifier.*;

/**
 *     An array of (potentially) mutable but non-replaceable objects.
 * <p>
 *     A structured array contains array element objects of a fixed (at creation time, per array instance) class,
 *     and can support elements of any class that provides accessible constructors. The elements in a StructuredArray
 *     are all allocated and constructed at array creation time, and individual elements cannot be removed or
 *     replaced after array creation. Array elements can be accessed using an index-based accessor methods in
 *     the form of {@link StructuredArray#get}() using either int or
 *     long indices. Individual element contents can then be accessed and manipulated using any and all
 *     operations supported by the member element's class.
 * <p>
 *     While simple creation of default-constructed elements and fixed constructor parameters are available through
 *     the newInstance factory methods, supporting arbitrary member types requires a wider range of construction
 *     options. The CtorAndArgsProvider API provides for array creation with arbitrary, user-supplied
 *     constructors and arguments, the selection of which can take the element index and construction context
 *     into account.
 * <p>
 *     StructuredArray is designed with semantics specifically chosen and restricted such that a "flat" memory
 *     layout of the implemented data structure would be possible on optimizing JVMs. Doing so provides for the
 *     possibility of matching access speed benefits that exist in data structures with similar semantics that
 *     are supported in other languages (e.g. an array of structs in C-like languages). While fully functional
 *     on all JVM implementation (of Java SE 6 and above), the semantics are such that a JVM may transparently
 *     optimise the implementation to provide a compact contiguous layout that facilitates consistent stride
 *     based memory access and dead-reckoning (as opposed to de-referenced) access to elements
 *
 * @param <T> The class of the array elements
 */
public class StructuredArray<T> extends StructuredArrayIntrinsifiableBase<T> implements Iterable<T> {

    private static final Object[] EMPTY_ARGS = new Object[0];

    final Field[] fields;
    final boolean hasFinalFields;

    private final StructuredArrayModel<? extends StructuredArray<T>, T> arrayModel;

    // Single-dimensional newInstance forms:

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    public static <T> StructuredArray<T> newInstance(
            final Class<T> elementClass,
            final long length) {
        try {
            final CtorAndArgsProvider<T> ctorAndArgsProvider = new ConstantCtorAndArgsProvider<T>(elementClass);
            return instantiate(elementClass, ctorAndArgsProvider, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param arrayClass of the array to create
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    public static <S extends StructuredArray<T>, T> S newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
            return newInstance(arrayCtorAndArgs, elementClass, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use <code>elementClass</code>'s default constructor.
     *
     * @param arrayCtorAndArgs for creating the array
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    public static <S extends StructuredArray<T>, T> S newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length) {
        try {
            StructuredArrayBuilder<S, T> arrayBuilder = new StructuredArrayBuilder<S, T>(
                    arrayCtorAndArgs.getConstructor().getDeclaringClass(),
                    elementClass,
                    length).
                    arrayCtorAndArgs(arrayCtorAndArgs).
                    resolve();
            return instantiate(arrayBuilder);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an array of <code>length</code> elements of type <code>elementClass</code>. Use constructor and
     * arguments supplied (on a potentially per element index basis) by the specified
     * <code>ctorAndArgsProvider</code> to construct and initialize each element.
     *
     * @param elementClass of each element in the array
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis
     * @param length of the array to create
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    public static <T> StructuredArray<T> newInstance(
            final Class<T> elementClass,
            final CtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) {
        try {
            return instantiate(elementClass, ctorAndArgsProvider, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Use constructor and arguments supplied (on a potentially
     * per element index basis) by the specified <code>ctorAndArgsProvider</code> to construct and initialize
     * each element.
     *
     * @param arrayClass of the array to create.
     * @param elementClass of each element in the array
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    public static <S extends StructuredArray<T>, T> S newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final CtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use constructor and arguments
     * supplied (on a potentially per element index basis) by the specified <code>ctorAndArgsProvider</code>
     * to construct and initialize each element.
     *
     * @param arrayCtorAndArgs of the array to create
     * @param elementClass of each element in the array
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    public static <S extends StructuredArray<T>, T> S newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final CtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) {
        try {
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a &ltS extends StructuredArray&ltT&gt&gt array instance according to the details provided in the
     * arrayBuilder.
     * @param arrayBuilder provides details for building the array
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    public static <S extends StructuredArray<T>, T> S newInstance(
            final StructuredArrayBuilder<S, T> arrayBuilder) {
        return instantiate(arrayBuilder);
    }

    /**
     * Copy a given array of elements to a newly created array. Copying of individual elements is done by using
     * the <code>elementClass</code> copy constructor to construct the individual member elements of the new
     * array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to duplicate
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     * @throws NoSuchMethodException if any contained element class does not support a copy constructor
     */
    public static <S extends StructuredArray<T>, T> S copyInstance(
            final S source) throws NoSuchMethodException {
        return copyInstance(source, 0, source.getLength());
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to copy from
     * @param sourceOffset offset index, indicating where the source region to be copied begins
     * @param count the number of elements to copy
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     * @throws NoSuchMethodException if any contained element class does not support a copy constructor
     */
    @SuppressWarnings("unchecked")
    public static <S extends StructuredArray<T>, T> S copyInstance(
            final S source,
            final long sourceOffset,
            final long count) throws NoSuchMethodException {
        return copyInstance(source, new long[] {sourceOffset}, new long[] {count});
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     * <p>
     * This form is useful [only] for copying partial ranges from nested StructuredArrays.
     * </p>
     * @param source The array to copy from
     * @param sourceOffsets offset indexes, indicating where the source region to be copied begins at each
     *                      StructuredArray nesting depth
     * @param counts the number of elements to copy at each StructuredArray nesting depth
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     * @throws NoSuchMethodException if any contained element class does not support a copy constructor
     */
    @SuppressWarnings("unchecked")
    public static <S extends StructuredArray<T>, T> S copyInstance(
            final S source,
            final long[] sourceOffsets,
            final long[] counts) throws NoSuchMethodException {
        if (sourceOffsets.length != counts.length) {
            throw new IllegalArgumentException("sourceOffsets.length must match counts.length");
        }

        // Verify source ranges fit in model:
        int depth = 0;
        StructuredArrayModel arrayModel = source.getArrayModel();
        while((depth < counts.length) && (arrayModel != null)) {
            if (arrayModel.getLength() < sourceOffsets[depth] + counts[depth]) {
                throw new ArrayIndexOutOfBoundsException(
                        "At nesting depth " + depth + ", source length (" + arrayModel.getLength() +
                                ") is smaller than sourceOffset (" + sourceOffsets[depth] +
                                ") + count (" + counts[depth] + ")" );
            }
            arrayModel = arrayModel.getSubArrayModel();
            depth++;
        }

        // If we run out of model depth before we run out of sourceOffsets and counts, throw:
        if (depth < counts.length) {
            throw new IllegalArgumentException("sourceOffsets.length and counts.length (" + counts.length +
                    ") must not exceed StructuredArray nesting depth (" + depth + ")");
        }

        final StructuredArrayModel<S, T> sourceArrayModel = (StructuredArrayModel<S, T>) source.getArrayModel();
        final Class<S> sourceArrayClass = sourceArrayModel.getArrayClass();
        Constructor<S> arrayConstructor = sourceArrayClass.getConstructor(sourceArrayClass);

        final StructuredArrayBuilder<S, T> arrayBuilder =
                createCopyingArrayBuilder(sourceArrayModel, sourceOffsets, 0, counts, 0).
                        arrayCtorAndArgs(arrayConstructor, source).
                        contextCookie(source);

        return instantiate(arrayBuilder);
    }

    private static <S extends StructuredArray<T>, T> StructuredArrayBuilder<S, T> createCopyingArrayBuilder(
            final StructuredArrayModel<S, T> sourceArrayModel,
            final long[] sourceOffsets, final int offsetsIndex,
            final long[] counts, final int countsIndex) throws NoSuchMethodException {
        final Class<S> sourceArrayClass = sourceArrayModel.getArrayClass();
        final Class<T> elementClass = sourceArrayModel.getElementClass();

        long sourceOffset = (offsetsIndex < sourceOffsets.length) ? sourceOffsets[offsetsIndex] : 0;
        long count = (countsIndex < counts.length) ? counts[countsIndex] : sourceArrayModel.getLength();

        final CtorAndArgsProvider<T> elementCopyCtorAndArgsProvider =
                new CopyCtorAndArgsProvider<T>(elementClass, sourceOffset);

        final StructuredArrayModel subArrayModel = sourceArrayModel.getSubArrayModel();

        if (subArrayModel != null) {
            // This array contains another array:
            StructuredArrayBuilder subArrayBuilder =
                    createCopyingArrayBuilder(subArrayModel,
                            sourceOffsets, offsetsIndex + 1,
                            counts, countsIndex + 1);
            return new StructuredArrayBuilder<S, T>(sourceArrayClass, subArrayBuilder, count).
                            elementCtorAndArgsProvider(elementCopyCtorAndArgsProvider).
                            resolve();
        } else {
            // This is a leaf array
            return new StructuredArrayBuilder<S, T>(sourceArrayClass, elementClass, count).
                    elementCtorAndArgsProvider(elementCopyCtorAndArgsProvider).
                    resolve();
        }
    }

    private static <T> StructuredArray<T> instantiate(
            final Class<T> elementClass,
            final CtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        StructuredArrayBuilder<StructuredArray<T>, T> arrayBuilder =
                new StructuredArrayBuilder(StructuredArray.class, elementClass, length).
                        elementCtorAndArgsProvider(ctorAndArgsProvider).resolve();
        return instantiate(arrayBuilder);
    }

    private static <S extends StructuredArray<T>, T> S instantiate(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final CtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        StructuredArrayBuilder<S, T> arrayBuilder =
                new StructuredArrayBuilder(arrayCtorAndArgs.getConstructor().getDeclaringClass(), elementClass, length).
                        arrayCtorAndArgs(arrayCtorAndArgs).
                        elementCtorAndArgsProvider(ctorAndArgsProvider).
                        resolve();
        return instantiate(arrayBuilder);
    }

    private static <S extends StructuredArray<T>, T> S instantiate(
            final StructuredArrayBuilder<S, T> arrayBuilder) {
        ConstructionContext<T> context = new ConstructionContext<T>(arrayBuilder.getContextCookie());
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(arrayBuilder, context);
        try {
            constructorMagic.setActive(true);
            StructuredArrayModel<S, T> arrayModel = arrayBuilder.getArrayModel();
            Constructor<S> constructor = arrayBuilder.getArrayCtorAndArgs().getConstructor();
            Object[] args = arrayBuilder.getArrayCtorAndArgs().getArgs();
            return StructuredArrayIntrinsifiableBase.instantiateStructuredArray(arrayModel, constructor, args);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    public StructuredArray() {
        checkConstructorMagic();
        // Extract locally needed args from constructor magic:
        ConstructorMagic constructorMagic = getConstructorMagic();
        @SuppressWarnings("unchecked")
        final ConstructionContext<T> context = constructorMagic.getContext();
        @SuppressWarnings("unchecked")
        final StructuredArrayBuilder<StructuredArray<T>, T> arrayBuilder = constructorMagic.getArrayBuilder();
        final CtorAndArgsProvider<T> ctorAndArgsProvider = arrayBuilder.getElementCtorAndArgsProvider();

        // Finish consuming constructMagic arguments:
        constructorMagic.setActive(false);

        context.setArray(this);
        this.arrayModel = arrayBuilder.getArrayModel();

        final Field[] fields = removeStaticFields(getElementClass().getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);

        StructuredArrayBuilder subArrayBuilder = arrayBuilder.getSubArrayBuilder();
        if (subArrayBuilder != null) {
            populateSubArrays(ctorAndArgsProvider, subArrayBuilder, context);
        } else {
            // This is a single dimension array. Populate it:
            populateLeafElements(ctorAndArgsProvider, context);
        }
    }

    public StructuredArray(StructuredArray<T> sourceArray) {
        // Support copy constructor. When we get here, everything is already set up for the regular
        // (default) construction path to perform the required copy.
        // Copying will actually be done according to the CtorAndArgsProvider and context already supplied,
        // with top-most source array being (already) passed as the contextCookie in the builder's
        // construction context, and copying proceeding using the context indices and the supplied
        // contextCookie provided by each CtorAndArgsProvider to figure out individual sources.
        this();
    }

    /**
     * Get the length (number of elements) of the array.
     *
     * @return the number of elements in the array.
     */
    public long getLength() {
        return super.getLength();
    }

    /**
     * Get the array model
     * @return a model of this array
     */
    public StructuredArrayModel<? extends StructuredArray<T>, T> getArrayModel() {
        return arrayModel;
    }

    /**
     * Get a reference to an element in a single dimensional array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public T get(final long index) throws IllegalArgumentException {
        return super.get(index);
    }

    /**
     * Get a reference to an element in a single dimensional array, using an <code>int</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public T get(final int index) throws IllegalArgumentException {
        return super.get(index);
    }


    private void populateElement(final long index,
                                 CtorAndArgs<T> ctorAndArgs) {
        try {
            // Instantiate:
            constructElementAtIndex(index, ctorAndArgs.getConstructor(), ctorAndArgs.getArgs());
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void populateSubArray(final ConstructionContext<T> context,
                                  StructuredArrayBuilder subArrayBuilder,
                                  final CtorAndArgs<T> subArrayCtorAndArgs) {
        ConstructionContext<T> subArrayContext = new ConstructionContext<T>(subArrayCtorAndArgs.getContextCookie());
        subArrayContext.setContainingContext(context);
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(subArrayBuilder, subArrayContext);
        try {
            constructorMagic.setActive(true);
            // Instantiate:
            constructSubArrayAtIndex(
                    context.getIndex(),
                    subArrayBuilder.getArrayModel(),
                    subArrayCtorAndArgs.getConstructor(),
                    subArrayCtorAndArgs.getArgs());
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    private void populateLeafElements(final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                      final ConstructionContext<T> context) {
        long length = getLength();

        try {
            for (long index = 0; index < length; index++) {
                final CtorAndArgs<T> ctorAndArgs;

                context.setIndex(index);
                ctorAndArgs = ctorAndArgsProvider.getForContext(context);

                if (ctorAndArgs.getConstructor().getDeclaringClass() != getElementClass()) {
                    throw new IllegalArgumentException("ElementClass (" + getElementClass() +
                            ") does not match ctorAndArgs.getConstructor().getDeclaringClass() (" +
                            ctorAndArgs.getConstructor().getDeclaringClass() + ")");
                }

                populateElement(index, ctorAndArgs);

                if (ctorAndArgsProvider instanceof AbstractCtorAndArgsProvider) {
                    ((AbstractCtorAndArgsProvider<T>) ctorAndArgsProvider).recycle(ctorAndArgs);
                }
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void populateSubArrays(final CtorAndArgsProvider<T> subArrayCtorAndArgsProvider,
                                   final StructuredArrayBuilder subArrayBuilder,
                                   final ConstructionContext<T> context) {
        long length = getLength();

        try {
            for (long index = 0; index < length; index++) {
                final CtorAndArgs<T> ctorAndArgs;

                context.setIndex(index);
                ctorAndArgs = subArrayCtorAndArgsProvider.getForContext(context);

                if (ctorAndArgs.getConstructor().getDeclaringClass() != getElementClass()) {
                    throw new IllegalArgumentException("ElementClass (" + getElementClass() +
                            ") does not match ctorAndArgs.getConstructor().getDeclaringClass() (" +
                            ctorAndArgs.getConstructor().getDeclaringClass() + ")");
                }

                populateSubArray(context, subArrayBuilder, ctorAndArgs);

                if (subArrayCtorAndArgsProvider instanceof AbstractCtorAndArgsProvider) {
                    ((AbstractCtorAndArgsProvider<T>) subArrayCtorAndArgsProvider).recycle(ctorAndArgs);                }
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the {@link Class} of elements stored in the array.
     *
     * @return the {@link Class} of elements stored in the array.
     */
    public Class<T> getElementClass() {
        return super.getElementClass();
    }

    /**
     * {@inheritDoc}
     */
    public ElementIterator iterator() {
        return new ElementIterator();
    }

    /**
     * Specialised {@link java.util.Iterator} with the ability to be {@link #reset()} enabling reuse.
     */
    public class ElementIterator implements Iterator<T> {

        private long cursor = 0;

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return cursor < getLength();
        }

        /**
         * {@inheritDoc}
         */
        public T next() {
            if (cursor >= getLength()) {
                throw new NoSuchElementException();
            }

            final T element = get(cursor);

            cursor++;

            return element;
        }

        /**
         * Remove operation is not supported on {@link StructuredArray}s.
         *
         * @throws UnsupportedOperationException if called.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Reset to the beginning of the collection enabling reuse of the iterator.
         */
        public void reset() {
            cursor = 0;
        }

        public long getCursor() {
            return cursor;
        }
    }

    private static Field[] removeStaticFields(final Field[] declaredFields) {
        int staticFieldCount = 0;
        for (final Field field : declaredFields) {
            if (isStatic(field.getModifiers())) {
                staticFieldCount++;
            }
        }

        final Field[] instanceFields = new Field[declaredFields.length - staticFieldCount];
        int i = 0;
        for (final Field field : declaredFields) {
            if (!isStatic(field.getModifiers())) {
                instanceFields[i++] = field;
            }
        }

        return instanceFields;
    }

    private boolean containsFinalQualifiedFields(final Field[] fields) {
        for (final Field field : fields) {
            if (isFinal(field.getModifiers())) {
                return true;
            }
        }

        return false;
    }

    private static void shallowCopy(final Object src, final Object dst, final Field[] fields) {
        try {
            for (final Field field : fields) {
                field.set(dst, field.get(src));
            }
        } catch (final IllegalAccessException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    private static void reverseShallowCopy(final Object src, final Object dst, final Field[] fields) {
        try {
            for (int i = fields.length - 1; i >= 0; i--) {
                final Field field = fields[i];
                field.set(dst, field.get(src));
            }
        } catch (final IllegalAccessException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    /**
     * Shallow copy a region of element object contents from one array to the other.
     * <p>
     * shallowCopy will copy all fields from each of the source elements to the corresponding fields in each
     * of the corresponding destination elements. If the same array is both the src and dst then the copy will
     * happen as if a temporary intermediate array was used.
     *
     * @param src array to copy
     * @param srcOffset offset index in src where the region begins
     * @param dst array into which the copy should occur
     * @param dstOffset offset index in the dst where the region begins
     * @param count of structure elements to copy
     * @param <S> The class of the arrays
     * @param <T> The class of the array elements
     * @throws IllegalArgumentException if the source and destination array element types are not identical, or if
     * the source or destination arrays have nested StructuredArrays within them, or if final fields are discovered
     * and all allowFinalFieldOverwrite is not true.
     */
    @SuppressWarnings("rawtypes")
    public static <S extends StructuredArray<T>, T> void shallowCopy(
            final S src,
            final long srcOffset,
            final S dst,
            final long dstOffset,
            final long count) {
        shallowCopy(src, srcOffset, dst, dstOffset, count, false);
    }

    /**
     * Shallow copy a region of element object contents from one array to the other.
     * <p>
     * shallowCopy will copy all fields from each of the source elements to the corresponding fields in each
     * of the corresponding destination elements. If the same array is both the src and dst then the copy will
     * happen as if a temporary intermediate array was used.
     *
     * If <code>allowFinalFieldOverwrite</code> is specified as <code>true</code>, even final fields will be copied.
     *
     * @param src array to copy
     * @param srcOffset offset index in src where the region begins
     * @param dst array into which the copy should occur
     * @param dstOffset offset index in the dst where the region begins
     * @param count of structure elements to copy.
     * @param allowFinalFieldOverwrite allow final fields to be overwritten during a copy operation.
     * @param <S> The class of the arrays
     * @param <T> The class of the array elements
     * @throws IllegalArgumentException if the source and destination array element types are not identical, or if
     * the source or destination arrays have nested StructuredArrays within them, or if final fields are discovered
     * and all allowFinalFieldOverwrite is not true.
     */
    @SuppressWarnings("rawtypes")
    public static <S extends StructuredArray<T>, T> void shallowCopy(
            final S src,
            final long srcOffset,
            final S dst,
            final long dstOffset,
            final long count,
            final boolean allowFinalFieldOverwrite) {
        if (src.getElementClass() != dst.getElementClass()) {
            String msg = String.format("Only objects of the same class can be copied: %s != %s",
                    src.getClass(), dst.getClass());
            throw new IllegalArgumentException(msg);
        }

        if ((StructuredArray.class.isAssignableFrom(src.getElementClass()) ||
                (StructuredArray.class.isAssignableFrom(dst.getElementClass())))) {
            throw new IllegalArgumentException("shallowCopy only supported for single dimension arrays (with no nested StructuredArrays)");
        }

        final Field[] fields = src.fields;
        if (!allowFinalFieldOverwrite && dst.hasFinalFields) {
            throw new IllegalArgumentException("Cannot shallow copy onto final fields");
        }

        if (((srcOffset + count) < Integer.MAX_VALUE) && ((dstOffset + count) < Integer.MAX_VALUE)) {
            // use the (faster) int based get
            if (dst == src && (dstOffset >= srcOffset && (dstOffset + count) >= srcOffset)) {
                int srcIdx = (int)(srcOffset + count) - 1;
                int dstIdx = (int)(dstOffset + count) - 1;
                int limit = (int)(srcOffset - 1);
                for (; srcIdx > limit; srcIdx--, dstIdx--) {
                    reverseShallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            } else {
                for (int srcIdx = (int)srcOffset, dstIdx = (int)dstOffset, limit = (int)(srcOffset + count);
                     srcIdx < limit; srcIdx++, dstIdx++) {
                    shallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            }
        } else {
            // use the (slower) long based getL
            if (dst == src && (dstOffset >= srcOffset && (dstOffset + count) >= srcOffset)) {
                for (long srcIdx = srcOffset + count, dstIdx = dstOffset + count, limit = srcOffset - 1;
                     srcIdx > limit; srcIdx--, dstIdx--) {
                    reverseShallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            } else {
                for (long srcIdx = srcOffset, dstIdx = dstOffset, limit = srcOffset + count;
                     srcIdx < limit; srcIdx++, dstIdx++) {
                    shallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            }
        }
    }

    //
    //
    // ConstructorMagic support:
    //
    //

    private static class ConstructorMagic {
        private boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;
        }

        public void setConstructionArgs(final StructuredArrayBuilder arrayBuilder, final ConstructionContext context) {
            this.arrayBuilder = arrayBuilder;
            this.context = context;
        }

        public StructuredArrayBuilder getArrayBuilder() {
            return arrayBuilder;
        }

        public ConstructionContext getContext() {
            return context;
        }

        private boolean active = false;

        private StructuredArrayBuilder arrayBuilder;
        private ConstructionContext context;
    }

    private static final ThreadLocal<ConstructorMagic> threadLocalConstructorMagic =
            new ThreadLocal<ConstructorMagic>() {
                @Override protected ConstructorMagic initialValue() {
                    return new ConstructorMagic();
                }
            };

    private static ConstructorMagic getConstructorMagic() {
        return threadLocalConstructorMagic.get();
    }

    private static void checkConstructorMagic() {
        if (!getConstructorMagic().isActive()) {
            throw new IllegalArgumentException(
                    "StructuredArray<> must not be directly instantiated with a constructor." +
                            " Use newInstance(...) or a builder instead.");
        }
    }
}
