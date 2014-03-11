package ch.claude_martin.enumbitset;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** EnumSet and BitSet in one mutable object. This holds a regular EnumSet, but adds more functions
 * to use it as a BitSet. Note that the implementation of EnumSet works like a bit set but the
 * interface doesn't allow to use it as such.
 * 
 * <p>
 * All information is hidden. But this class offers many more methods compared to {@link EnumSet}.
 * Those extra methods are inspired by set theory so that set operations are much simpler to perform
 * with the given methods.
 * 
 * <p>
 * Methods such as {@link #union(EnumBitSet)}, {@link #toEnumSet()}, and {@link #complement()}
 * return a new and independent set. This allows a functional style of programming.
 * 
 * However, this set is mutable. It can be altered using the interface of {@link Set} (
 * {@link #add(Enum)}, {@link #remove(Object)} etc.). This allows the classic imperative style of
 * programming.
 * 
 * <p>
 * Sadly both BitSet and EnumSet are not implementations of some interface. Therefore this class can
 * not share the interface of these other classes. Most methods are implemented here so this is in
 * fact compatible in most cases.
 * 
 * <p>
 * This set is a {@link DomainBitSet} with a domain made of all enum elements of a given enum type.
 * 
 * <p>
 * This set is not thread-safe. You can use {@link Collections#unmodifiableSet(Set)}, but will lose
 * all methods not declared in {@link java.util.Set}.
 * 
 * <p>
 * See the <a href="./package-info.html">package-info</a> for naming conventions.
 * 
 * @author <a href="http://claude-martin.ch/enumbitset/">Copyright &copy; 2014 Claude Martin</a>
 * 
 * @param <E>
 *          Enum type that implements <code>{@link EnumBitSetHelper}&lt;E&gt; </code>. */
public final class EnumBitSet<E extends Enum<E> & EnumBitSetHelper<E>> implements DomainBitSet<E>,
    Set<E> {

  /** Creates an EnumBitSet containing all of the elements in the specified element type.
   * 
   * @see #of(Enum, Enum...)
   * @see #noneOf(Class)
   * @param <X>
   *          The enum type.
   * @param type
   *          Enum type.
   * @return EnumBitSet containing all elements. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> allOf(final Class<X> type) {
    return new EnumBitSet<>(type, EnumSet.allOf(type));
  }

  /** Convert EnumSet to BitInteger.
   * 
   * @param <X>
   *          Enum type of the elements.
   * @param set
   *          A set of enum constants.
   * @return A BigInteger that represents the given set as a bit mask. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> BigInteger asBigInteger(
      final EnumSet<X> set) {
    return BitSetUtilities.asBigInteger(asBitSet(set));
  }

  /** Convert VarArg/Array of enums to BitInteger.
   * 
   * @param <X>
   *          Enum type of the elements.
   * @param set
   *          A set of enum constants.
   * @return A BigInteger that represents the given set as a bit mask. */
  @SafeVarargs
  public static <X extends Enum<X> & EnumBitSetHelper<X>> BigInteger asBigInteger(final X... set) {
    return BitSetUtilities.asBigInteger(asBitSet(set));
  }

  /** Creates a BitSet of a given set of enums.
   * 
   * @param <X>
   *          Enum type of the elements.
   * @param set
   *          A set of enum constants.
   * @return New BitSet, equal to the given EnumSet. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> BitSet asBitSet(final EnumSet<X> set) {
    final BitSet result = new BitSet(64);
    for (final X e : requireNonNull(set))
      result.set(e.ordinal());
    return result;
  }

  /** Creates a BitSet of a given set of enums.
   * 
   * @param <X>
   *          Enum type of the elements.
   * @param set
   *          A set of enum constants.
   * @return A BitSet that represents the given set. */
  @SafeVarargs
  public static <X extends Enum<X> & EnumBitSetHelper<X>> BitSet asBitSet(final X... set) {
    final BitSet result = new BitSet();
    for (final X e : requireNonNull(set))
      result.set(e.ordinal());
    return result;
  }

  /** Creates set of enums from a bit set.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param mask
   *          A bit mask, must be positive.
   * @param type
   *          The class of the parameter elements and of the set.
   * @return New EnumBitSet, equal to the given bit mask. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> asEnumBitSet(
      final BigInteger mask, final Class<X> type) {
    if (requireNonNull(mask).signum() == -1)
      throw new IllegalArgumentException("The mask must not be negative!");
    return new EnumBitSet<>(type, asEnumSet(mask, type));
  }

  /** Creates set of enums from a bit set.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param set
   *          A BitSet.
   * @param type
   *          The class of the parameter elements and of the set.
   * @return New EnumBitSet, equal to the BitSet. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> asEnumBitSet(
      final BitSet set, final Class<X> type) {
    return new EnumBitSet<>(type, asEnumSet(set, type));
  }

  /** Creates a new EnumBitSet from a given Collection. The Collection must not contain
   * <code>null</code>.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param collection
   *          A Collection of enum elements, not null and not containing null.
   * @param type
   *          The class of the parameter elements and of the set.
   * @return New EnumBitSet, equal to the Collection. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> asEnumBitSet(
      final Collection<X> collection, final Class<X> type) {
    final EnumBitSet<X> result = noneOf(type);
    result.addAll(collection);
    return result;
  }

  /** Creates a new EnumBitSet from a given BitSet.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param set
   *          An EnumSet.
   * @param type
   *          The class of the parameter elements and of the set.
   * @return New EnumBitSet, equal to the EnumSet. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> asEnumBitSet(
      final EnumSet<X> set, final Class<X> type) {
    return new EnumBitSet<>(type, set.clone());
  }

  /** Creates set of enums from a long.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param mask
   *          A bit mask.
   * @param type
   *          The class of the parameter elements and of the set.
   * @return New EnumBitSet, equal to the given bit mask. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> asEnumBitSet(
      final long mask, final Class<X> type) throws MoreThan64ElementsException {
    return new EnumBitSet<>(type, asEnumSet(mask, type));
  }

  /** Creates set of enums from a bit set.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param mask
   *          A bit mask, must be positive.
   * @param type
   *          The class of the parameter elements and of the set
   * @return New EnumSet, equal to the given bit mask. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumSet<X> asEnumSet(
      final BigInteger mask, final Class<X> type) {
    if (requireNonNull(mask).signum() == -1)
      throw new IllegalArgumentException("The mask must not be negative!");
    final EnumSet<X> result = EnumSet.allOf(requireNonNull(type));
    result.removeIf(e -> e.intersect(mask).equals(BigInteger.ZERO));
    return result;
  }

  /** Creates set of enums from a BitSet.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param bitset
   *          A BitSet.
   * @param type
   *          The class of the parameter elements and of the set.
   * @return New EnumSet, equal to the given BitSet. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumSet<X> asEnumSet(final BitSet bitset,
      final Class<X> type) {
    requireNonNull(bitset);
    final EnumSet<X> result = EnumSet.allOf(requireNonNull(type));
    result.removeIf(e -> !bitset.get(e.ordinal()));
    return result;
  }

  /** Creates set of enums from a 64 bit bit set.
   * 
   * @param <X>
   *          The enum type of the set.
   * @param mask
   *          The bit mask.
   * @param type
   *          The enum type of the set.
   * @throws MoreThan64ElementsException
   *           This fails if the enum type contains more than 64 elements.
   * @return New EnumSet with all elements of the given bit mask. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumSet<X> asEnumSet(final long mask,
      final Class<X> type) throws MoreThan64ElementsException {
    final EnumSet<X> result = EnumSet.allOf(requireNonNull(type));
    result.removeIf(e -> (e.bitmask64() & mask) == 0);
    return result;
  }

  /** Creates set of enums from at least one element.
   * 
   * It is recommended to use {@link EnumSet#of(Enum, Enum...)} directly!
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param first
   *          an element that the set is to contain initially
   * @param rest
   *          the remaining elements the set is to contain initially
   * @throws NullPointerException
   *           if any of the specified elements are null, or if <tt>rest</tt> is null
   * @return an EnumSet initially containing the specified elements. This is equal to:
   *         <code>EnumSet.of(first, rest)</code>. */
  @SafeVarargs
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumSet<X> asEnumSet(final X first,
      final X... rest) {
    return EnumSet.of(first, rest);
  }

  /** Creates a 64 bit bitmask of a given set of enums.
   * 
   * @param <X>
   *          Enum type of the elements.
   * @param set
   *          An EnumSet af an enum type with up to 64 constants.
   * @throws MoreThan64ElementsException
   *           This fails if any element in the set has a higher index than 63.
   * @return A long value that represents the given set as a bit mask. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> long asLong(final EnumSet<X> set)
      throws MoreThan64ElementsException {
    long result = 0L;
    for (final X x : requireNonNull(set))
      result |= x.bitmask64();
    return result;
  }

  /** Creates a 64 bit bit set of a given set of enums.
   * 
   * @param <X>
   *          Enum type of the elements.
   * @param set
   *          An EnumSet af an enum type with up to 64 constants.
   * @throws MoreThan64ElementsException
   *           This fails if any element in the set has a higher index than 63.
   * @return A long value that represents the given set as a bit mask. */
  @SafeVarargs
  public static <X extends Enum<X> & EnumBitSetHelper<X>> long asLong(final X... set)
      throws MoreThan64ElementsException {
    long result = 0;
    for (final X e : requireNonNull(set))
      result |= e.bitmask64();// bitmask64() checks index!
    return result;
  }

  /** Returns a new EnumBitSet containing just one enum value.<br>
   * Note: <code>EnumBitSet.just(X)</code> is equal to <code>X.asEnumBitSet()</code>
   * 
   * @param <X>
   *          The enum type of the value.
   * @param value
   *          The single value that will be contained in the result.
   * @return New EnumBitSet containing nothing but <code>value</code>. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> just(final X value) {
    final EnumBitSet<X> result = noneOf(requireNonNull(value).getDeclaringClass());
    result.add(value);
    return result;
  }

  /** Creates an EnumBitSet containing none of the elements in the specified element type.
   * 
   * @see #of(Enum, Enum...)
   * @see #allOf(Class)
   * @param <X>
   *          The enum type.
   * @param type
   *          Enum type.
   * @return EnumBitSet containing no elements. */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> noneOf(final Class<X> type) {
    return new EnumBitSet<>(type);
  }

  /** Creates a new EnumBitSet containing at least one value.
   * 
   * @param <X>
   *          The enum type of all elements.
   * @param first
   *          The first element (must not be null).
   * @param more
   *          More elements to add.
   * @return New EnumBitSet containing all given elements.
   * @see #noneOf(Class) */
  @SafeVarargs
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> of(final X first,
      final X... more) {
    final EnumBitSet<X> result = noneOf(requireNonNull(first).getDeclaringClass());
    result.add(first);
    if (more != null)
      for (final X x : more)
        result.add(x);
    return result;
  }

  /** Creates an enum set initially containing all of the elements in the range defined by the two
   * specified endpoints. The returned set will contain the endpoints themselves, which may be
   * identical but must not be out of order.
   * 
   * @param <X>
   *          The class of the parameter elements and of the set
   * @param from
   *          the first element in the range
   * @param to
   *          the last element in the range
   * @throws NullPointerException
   *           if {@code from} or {@code to} are null
   * @throws IllegalArgumentException
   *           if {@code from.compareTo(to) > 0}
   * @return an enum set initially containing all of the elements in the range defined by the two
   *         specified endpoints */
  public static <X extends Enum<X> & EnumBitSetHelper<X>> EnumBitSet<X> range(final X from,
      final X to) {
    return new EnumBitSet<>(requireNonNull(from).getDeclaringClass(), EnumSet.range(from,
        requireNonNull(to)));
  }

  private final EnumSet<E>                                                                         bitset;

  private final Class<E>                                                                           enumType;

  // Note that is is intentionally not marked as "volatile". Visibility is guaranteed by JMM.
  private volatile int                                                                             enumTypeSize = -1;

  private Domain<E>                                                                                domain       = null;
  private static final HashMap<Class<? extends Enum<?>>, SoftReference<Domain<? extends Enum<?>>>> domainCache  = new HashMap<>();

  private EnumBitSet(final Class<E> type) {
    this(type, EnumSet.noneOf(type));
  }

  private EnumBitSet(final Class<E> type, final EnumSet<E> set) {
    this.enumType = requireNonNull(type);
    this.bitset = requireNonNull(set);
  }

  /** {@inheritDoc} */
  @Override
  public boolean add(final E e) {
    return this.bitset.add(e);
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(final Collection<? extends E> c) {
    return this.bitset.addAll(c);
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    this.bitset.clear();
  }

  /** Returns a copy of this set. */
  @Override
  public EnumBitSet<E> clone() {
    return new EnumBitSet<>(this.enumType, this.bitset.clone());
  }

  /** Creates a new EnumBitSet with the same element type as this, initially containing all the
   * elements of this type that are not contained in this set.
   * 
   * @return The complement of this EnumBitSet. */
  @Override
  public EnumBitSet<E> complement() {
    return new EnumBitSet<>(this.enumType, EnumSet.complementOf(this.bitset));
  }

  /** {@inheritDoc} */
  @Override
  public boolean contains(final Object o) {
    return this.bitset.contains(o);
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsAll(final Collection<?> c) {
    return this.bitset.containsAll(c);
  }

  /** The Cartesian product with another EnumBitSet. This overload only differs in that its generic
   * return type uses {@link Enum} and {@link EnumBitSetHelper} instead of Object.
   * 
   * @param set
   *          Another set.
   * @return an {@link ArrayList} containing all {@link Pair pairs}.
   * @see DomainBitSet#cross(DomainBitSet, BiFunction) */
  public <Y extends Enum<Y> & EnumBitSetHelper<Y>> List<Pair<EnumBitSetHelper<?>, E, Y>> cross(
      final EnumBitSet<Y> set) {
    final ArrayList<Pair<EnumBitSetHelper<?>, E, Y>> result = new ArrayList<>(this.size()
        * set.size());
    // Could be written like this:
    // this.cross(set, Pair::of.andThen(result::add));
    // But javac can't infer the type, but this would work:
    // this.cross(set, ((BiFunction<E, Y, Pair<EnumBitSetHelper<?>, E, Y>>)
    // Pair::of).andThen(result::add));
    // However, this looks much nicer:
    this.cross(set, (x, y) -> result.add(Pair.of(x, y)));
    return result;
  }

  /** Is the given enum constant an element of this set's domain?
   * 
   * @return <code>object.getDeclaringClass() == this.enumType</code> */
  @Override
  public boolean domainContains(final E object) {
    return object.getDeclaringClass() == this.enumType;
  }

  /** {@inheritDoc}
   * 
   * @see EnumSet#equals(Object) */
  @Override
  public boolean equals(final Object obj) {
    return this.bitset.equals(obj);
  }

  @Override
  public void forEach(final Consumer<? super E> action) {
    requireNonNull(action);
    for (final E e : this.bitset)
      action.accept(e);
  }

  @Override
  public boolean getBit(final int bitIndex) throws IndexOutOfBoundsException {
    final int size = this.getEnumTypeSize();
    if (bitIndex < 0 || bitIndex >= size)
      throw new IndexOutOfBoundsException();
    if (size <= 64)
      return (this.toLong() >>> bitIndex) % 2 == 1;
    // Using getDomain should be faster as the domain is cached.
    final boolean result = this.bitset.contains(this.getDomain().get(bitIndex));
    assert result == this.toBitSet().get(bitIndex);
    return result;
  }

  /** The Domain containing all elements of the enum type.
   * 
   * @see #getEnumTypeSize()
   * @return <code>Domain</code> with all enum elements. */
  @SuppressWarnings("unchecked")
  @Override
  public Domain<E> getDomain() {
    if (this.domain == null)
      synchronized (domainCache) {
        if (null == this.domain) {
          final SoftReference<Domain<? extends Enum<?>>> SoftReference = domainCache
              .get(this.enumType);
          if (SoftReference != null)
            this.domain = (Domain<E>) SoftReference.get();
          if (null == this.domain) {
            this.domain = new DefaultDomain<>(asList(this.enumType.getEnumConstants()));
            domainCache.put(this.enumType,
                new SoftReference<Domain<? extends Enum<?>>>(this.domain));
          }
          assert this.enumTypeSize == -1 || this.enumTypeSize == this.domain.size();
          this.enumTypeSize = this.domain.size();
        }
      }
    return this.domain;
  }

  /** The enum type class that defines the available enum elements. This is the class returned by
   * {@link Enum#getDeclaringClass()}.
   * 
   * @return The declaring class of all elements in this set. */
  public Class<E> getEnumType() {
    return this.enumType;
  }

  /** Amount of enum elements. This is relevant to know how large the bit field must be to hold a bit
   * set of this type.
   * <p>
   * This is euqal to <code>{@link #getDomain()}.size()</code>, but possibly faster.
   * 
   * @return Number of constants of the enum type. */
  public int getEnumTypeSize() {
    if (this.enumTypeSize == -1) {
      // this.enumTypeSize = this.enumType.getEnumConstants().length;
      this.getDomain(); // This also sets this.enumTypeSize!
      synchronized (domainCache) {
        // JMM guarantees visibility:
        return this.enumTypeSize;
      }
    }
    return this.enumTypeSize;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return this.bitset.hashCode();
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> and the given
   * <code>mask</code>.
   * 
   * @see #complement()
   * @param mask
   *          Another set, represented by a bit mask, must be positive.
   * @return <code> this &#x2229; set</code> */
  @Override
  public EnumBitSet<E> intersect(final BigInteger mask) {
    if (requireNonNull(mask).signum() == -1)
      throw new IllegalArgumentException("The mask must not be negative!");
    final EnumSet<E> clone = this.bitset.clone();
    clone.removeIf(e -> !mask.testBit(e.ordinal()));
    return new EnumBitSet<>(this.enumType, clone);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> and the given
   * <code>set</code>.
   * 
   * @see #complement()
   * @param set
   *          Another set.
   * @return <code> this &#x2229; set</code> */
  @Override
  public EnumBitSet<E> intersect(final BitSet set) {
    final EnumSet<E> clone = this.bitset.clone();
    clone.removeIf(e -> !set.get(e.ordinal()));
    return new EnumBitSet<>(this.enumType, clone);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> and the given
   * <code>set</code>.
   * 
   * @param set
   *          Another set.
   * @see #complement()
   * @return <code> this &#x2229; set</code> */
  public EnumBitSet<E> intersect(final EnumBitSet<E> set) {
    return this.intersect(set.toEnumSet());
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> and the given
   * <code>set</code>.
   * 
   * @see #complement()
   * @param set
   *          Another set.
   * @return <code> this &#x2229; set</code> */
  public EnumBitSet<E> intersect(final EnumSet<E> set) {
    final EnumSet<E> clone = this.bitset.clone();
    clone.retainAll(set);
    return new EnumBitSet<>(this.enumType, clone);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> and the given
   * <code>set</code>.
   * 
   * @see #complement()
   * @param mask
   *          Another set..
   * @return <code> this &#x2229; set</code> */
  @Override
  public DomainBitSet<E> intersect(final Iterable<E> set) {
    if (requireNonNull(set) instanceof EnumBitSet)
      return this.intersect(((EnumBitSet<E>) set).bitset);
    final EnumSet<E> result = this.bitset.clone();
    set.forEach(result::remove);
    return new EnumBitSet<>(this.enumType, result);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> and the given
   * <code>mask</code>.
   * 
   * @see #complement()
   * @param mask
   *          Another set, represented by a bit mask.
   * @throws MoreThan64ElementsException
   *           This fails if any element in the set has a higher index than 63.
   * @return <code> this &#x2229; set</code> */
  @Override
  public EnumBitSet<E> intersect(final long mask) throws MoreThan64ElementsException {
    final EnumSet<E> clone = this.bitset.clone();
    clone.removeIf(e -> (e.bitmask64() & mask) == 0);
    return new EnumBitSet<>(this.enumType, clone);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> and the given
   * <code>set</code>.
   * 
   * @see #complement()
   * @param set
   *          Another set.
   * @return <code> this &#x2229; set</code> */
  @Override
  public EnumBitSet<E> intersectVarArgs(@SuppressWarnings("unchecked") final E... set) {
    final EnumSet<E> clone = this.bitset.clone();
    clone.retainAll(asList(set));
    return new EnumBitSet<>(this.enumType, clone);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmpty() {
    return this.bitset.isEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<E> iterator() {
    return this.bitset.iterator();
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code>, but not in the
   * given <code>mask</code>.
   * 
   * @see #removeAll(Collection)
   * @param mask
   *          Another set, represented by a bit mask, must be positive.
   * @return <code>this &#x2216; mask</code> */
  @Override
  public EnumBitSet<E> minus(final BigInteger mask) {
    // A\B = A & ~B
    // So one might think that this works:
    // this.toBigInteger().and(mask.not());
    // It doesn't, because the BigInteger doesn't know the size of the enum
    // type (the "domain").

    // Solution using BigInteger:
    final BigInteger self = this.toBigInteger();
    final BigInteger one = BigInteger.ONE;
    final BigInteger all = one.shiftLeft(this.getEnumTypeSize()).subtract(one);
    final BigInteger notb = mask.xor(all);
    return asEnumBitSet(self.and(notb), this.enumType);

    // Solution using EnumSet:
    // final EnumBitSet<E> result = this.clone();
    // if (BigInteger.ZERO.equals(mask))
    // return result;
    // result.removeAll(asEnumSet(mask, this.enumType));
    // return result;
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code>, but not in the
   * given <code>bit set</code>.
   * 
   * @see #removeAll(Collection)
   * @param set
   *          Another set.
   * @return <code>this &#x2216; set</code> */
  @Override
  public EnumBitSet<E> minus(final BitSet set) {
    final EnumBitSet<E> result = this.clone();
    if (set == null || set.isEmpty())
      return result;
    result.removeAll(asEnumSet(set, this.enumType));
    return result;
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code>, but not in the
   * given <code>set</code>.
   * 
   * @see #removeAll(Collection)
   * @param set
   *          Another set.
   * @return <code>this &#x2216; set</code> */
  public EnumBitSet<E> minus(final EnumBitSet<E> set) {
    final EnumBitSet<E> result = this.clone();
    if (set == null || set.isEmpty())
      return result;
    result.removeAll(set);
    return result;
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code>, but not in the
   * given <code>set</code>.
   * 
   * @see #removeAll(Collection)
   * @param set
   *          Another set.
   * @return <code>this &#x2216; set</code> */
  public EnumBitSet<E> minus(final EnumSet<E> set) {
    final EnumBitSet<E> result = this.clone();
    if (requireNonNull(set).isEmpty())
      return result;
    result.removeAll(set);
    return result;
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code>, but not in the
   * given <code>set</code>.
   * 
   * @see #removeAll(Collection)
   * @param set
   *          Another set.
   * @return <code>this &#x2216; mask</code> */
  @Override
  public DomainBitSet<E> minus(final Iterable<E> set) {
    if (requireNonNull(set) instanceof EnumBitSet)
      return this.intersect(((EnumBitSet<E>) set).bitset);
    final EnumBitSet<E> result = this.clone();
    set.forEach(result.bitset::remove);
    return result;
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code>, but not in the
   * given <code>mask</code>.
   * 
   * @see #removeAll(Collection)
   * @param mask
   *          Another set, represented by a bit mask.
   * @throws MoreThan64ElementsException
   *           This fails if any element in this set has a higher index than 63.
   * @return <code>this &#x2216; mask</code> */
  @Override
  public EnumBitSet<E> minus(final long mask) throws MoreThan64ElementsException {
    if (mask == 0)
      return this.clone();
    return asEnumBitSet(this.toLong() & ~mask, this.enumType);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code>, but not in the
   * given <code>set</code>.
   * 
   * @see #removeAll(Collection)
   * @param set
   *          Another set.
   * @return <code>this &#x2216; set</code> */
  @Override
  @SuppressWarnings("unchecked")
  public EnumBitSet<E> minusVarArgs(final E... set) {
    final EnumBitSet<E> result = this.clone();
    if (set == null || set.length == 0)
      return result;
    result.removeAll(asList(set));
    return result;
  }

  @Override
  public boolean ofEqualDomain(final DomainBitSet<E> set) {
    if (requireNonNull(set) instanceof EnumBitSet)
      return this.enumType == ((EnumBitSet<?>) set).enumType;
    // unlikely but possible:
    return this.getDomain().equals(set.getDomain());
  }

  /** {@inheritDoc} */
  @Override
  public boolean remove(final Object o) {
    return this.bitset.remove(o);
  }

  /** {@inheritDoc}
   * 
   * @see #minusVarArgs(Enum...)
   * @see #minus(EnumBitSet)
   * @see #minus(EnumSet)
   * @see #minus(BigInteger) */
  @Override
  public boolean removeAll(final Collection<?> c) {
    return this.bitset.removeAll(c);
  }

  /** {@inheritDoc}
   * 
   * @see #intersect(BigInteger)
   * @see #intersect(BitSet)
   * @see #intersectVarArgs(Enum...)
   * @see #intersect(EnumBitSet)
   * @see #intersect(EnumSet)
   * @see #intersect(long) */
  @Override
  public boolean retainAll(final Collection<?> c) {
    return this.bitset.retainAll(c);
  }

  /** {@inheritDoc}
   * <p>
   * Not to be confused with {@link #getEnumTypeSize()}. */
  @Override
  public int size() {
    return this.bitset.size();
  }

  /** {@inheritDoc} */
  @Override
  public Spliterator<E> spliterator() {
    return this.bitset.spliterator();
  }

  /** {@inheritDoc} */
  @Override
  public Object[] toArray() {
    return this.bitset.toArray();
  }

  /** {@inheritDoc} */
  @Override
  public <T> T[] toArray(final T[] a) {
    return this.bitset.toArray(a);
  }

  /** Returns a BigInteger that represents this set.
   * 
   * @see #toBinaryString()
   * @return A representation of this {@link EnumBitSet} as a {@link BigInteger} . */
  @Override
  public BigInteger toBigInteger() {
    if (this.getEnumTypeSize() <= 64)
      return BitSetUtilities.asBigInteger(this.toLong());
    else
      return BitSetUtilities.asBigInteger(this.toBitSet());
  }

  /** Binary string representation of this set.
   * <p>
   * The length of the returned string is the same as the amount of enum elements in the enum type.
   * 
   * @return A representation of this {@link EnumBitSet} as a String of 0s and 1s. */
  public String toBinaryString() {
    return this.toBinaryString(this.getEnumTypeSize());
  }

  /** Binary string representation of this set.
   * <p>
   * The length of the returned string is as least as long as <i>width</i>.
   * <p>
   * Example: Use this if you have an enum type with less than 64 elements but you want to use a bit
   * field with 64 bits. <code>this.toBinaryString(64)</code> will already have the appropriate
   * length.
   * 
   * @param width
   *          The minimal width of the returned String.
   * @return A representation of this {@link EnumBitSet} as a String of 0s and 1s. The length is at
   *         least <i>width</i>. */
  public String toBinaryString(final int width) {
    final String binary = this.toBigInteger().toString(2);
    final StringBuilder sb = new StringBuilder(width < 8 ? 8 : width);
    while (sb.length() < width - binary.length())
      sb.append('0');
    sb.append(binary);
    return sb.toString();
  }

  /** Returns a new BitSet that represents this set.
   * 
   * @return A representation of this {@link EnumBitSet} as a {@link BitSet}; */
  @Override
  public BitSet toBitSet() {
    final BitSet result = new BitSet(this.getEnumTypeSize());
    for (final E e : this.bitset)
      result.set(e.ordinal());
    return result;
  }

  /** Copy of the underlying EnumSet.
   * 
   * @return <code>bitset.clone()</code> */
  public EnumSet<E> toEnumSet() {
    return this.bitset.clone();
  }

  /** Returns a long value that represents this set.
   * 
   * @throws MoreThan64ElementsException
   *           This fails if any element in this set has a higher index than 63.
   * @return A representation of this {@link EnumBitSet} as a {@link Long long}. */
  @Override
  public long toLong() throws MoreThan64ElementsException {
    long result = 0L;
    for (final E e : this.bitset)
      result |= e.bitmask64();// bitmask64() checks index!
    return result;
  }

  /** Copy of the underlying EnumSet.
   * 
   * @return <code>bitset.clone()</code> */
  @Override
  public Set<E> toSet() {
    return this.bitset.clone();
  }

  /** Returns a string representation of this set.
   * 
   * @see EnumSet#toString() */
  @Override
  public String toString() {
    return this.bitset.toString();
  }

  /** Throws an exception if e is not of the correct type for this enum set.
   * 
   * @param e
   *          the element to be checked.
   * @throws ClassCastException
   *           Throws ClassCastException if e is not of the correct type. */
  public void typeCheck(final E e) throws ClassCastException {
    // This is equivalent to the nonvisible method: this.bitset.typeCheck(e);
    final Class<?> eClass = e.getClass();
    // note that this would be faster:
    // if (eClass != elementType && eClass.getSuperclass() != elementType)
    // But the API only guarantees that this works:
    if (eClass.getDeclaringClass() != this.enumType)
      throw new ClassCastException(eClass + " != " + this.enumType);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> or the given
   * <code>mask</code>.
   * 
   * @param mask
   *          Bit mask of another set, must be positive.
   * @return <code> this &#x222a; set</code>
   * @throws IllegalArgumentException
   *           The bit mask must not have any bits set that are not mapped to an enum constant. */
  @Override
  public EnumBitSet<E> union(final BigInteger mask) {
    if (requireNonNull(mask).signum() == -1)
      throw new IllegalArgumentException("The mask must not be negative!");
    final EnumBitSet<E> result = this.clone();
    final Domain<E> dom = this.getDomain();
    // Test that mask does not contain more than domain allows:
    if (0 != mask.shiftRight(dom.size()).signum())
      throw new IllegalArgumentException("The given mask is not applicable to this set.");
    for (int i = 0; i < dom.size(); i++)
      if (mask.testBit(i))
        result.add(dom.get(i));
    assert this.union(asEnumSet(mask, this.enumType)).equals(result) : "Error in union(BigInteger)";
    return result;
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> or the given
   * <code>set</code>.
   * 
   * @param set
   *          Another set.
   * @return <code> this &#x222a; set</code>
   * @throws IllegalArgumentException
   *           The bit set must not have any bits set that are not mapped to an enum constant. */
  @Override
  public EnumBitSet<E> union(final BitSet set) {
    // return union(asEnumSet(set, this.enumType));
    final Domain<E> dom = this.getDomain();
    if (-1 != set.nextSetBit(dom.size()))
      throw new IllegalArgumentException("The given mask is not applicable to this set.");
    final EnumSet<E> clone = this.bitset.clone();
    for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1))
      clone.add(dom.get(i));// The above check should ensure that no IOOBE is thrown here.
    return new EnumBitSet<>(this.enumType, clone);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> or the given
   * <code>set</code>.
   * 
   * @param set
   *          Another set.
   * @return <code> this &#x222a; set</code> */
  public EnumBitSet<E> union(final EnumBitSet<E> set) {
    return this.union(set.bitset);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> or the given
   * <code>set</code>.
   * 
   * @param set
   *          Another set.
   * @return <code> this &#x222a; set</code> */
  public EnumBitSet<E> union(final EnumSet<E> set) {
    final EnumSet<E> clone = this.bitset.clone();
    clone.addAll(set);
    return new EnumBitSet<>(this.enumType, clone);
  }

  @Override
  public DomainBitSet<E> union(final Iterable<E> set) {
    if (set instanceof EnumBitSet)
      return this.union(((EnumBitSet<E>) set).bitset);
    final EnumSet<E> clone = this.bitset.clone();
    set.forEach(clone::add);
    return new EnumBitSet<>(this.enumType, clone);
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> or the given
   * <code>mask</code>.
   * 
   * @param mask
   *          Bit mask of another set.
   * @throws MoreThan64ElementsException
   *           This fails if any element in this set has a higher index than 63.
   * @return <code> this &#x222a; set</code> */
  @Override
  public EnumBitSet<E> union(final long mask) throws MoreThan64ElementsException {
    return this.union(asEnumSet(mask, this.enumType));
  }

  /** Returns a new EnumBitSet containing all elements that are in <code>this</code> or the given
   * <code>set</code>.
   * 
   * @param set
   *          Another set.
   * @return <code> this &#x222a; set</code> */
  @Override
  public EnumBitSet<E> unionVarArgs(@SuppressWarnings("unchecked") final E... set) {
    final EnumSet<E> clone = this.bitset.clone();
    clone.addAll(asList(set));
    return new EnumBitSet<>(this.enumType, clone);
  }

}