# Kyo's Maybe Implementation Analysis

## Key Insights

### 1. Opaque Type Encoding
Kyo uses an opaque type definition to achieve zero-allocation performance:
```scala
opaque type Maybe[+A] >: (Absent | Present[A]) = Absent | Present[A]
```

### 2. Nested Empty Values Problem Solution
The brilliant solution to the `Some(None)` problem uses a `PresentAbsent` class:
```scala
case class PresentAbsent(val depth: Int):
    def unnest = 
        if depth > 1 then PresentAbsent(depth - 1)
        else Absent
    def nest = PresentAbsent(depth + 1)
```

This allows representing nested empty values with a depth counter:
- `Maybe.empty[Maybe[Int]]` → `Absent`
- `Maybe(Maybe.empty[Int])` → `PresentAbsent(1)`
- `Maybe(Maybe(Maybe.empty[Int]))` → `PresentAbsent(2)`

### 3. Core Type Structure
```scala
sealed abstract class Absent
case object Absent extends Absent
opaque type Present[+A] = A | PresentAbsent
```

The encoding uses:
- `Absent` as a singleton for empty values
- `Present[A]` as either the actual value `A` or `PresentAbsent` for nested empties

### 4. Smart Constructors
```scala
def apply[A](v: A): Maybe[A] =
    if isNull(v) then Absent
    else if v.isInstanceOf[Absent] then PresentAbsent(1)
    else if v.isInstanceOf[PresentAbsent] then 
        v.asInstanceOf[PresentAbsent].nest
    else v
```

The constructor handles:
- Null values → Absent
- Nested Absent values → PresentAbsent with appropriate depth
- Regular values → stored directly

### 5. Key Operations Implementation
All operations are `transparent inline` for zero overhead:
```scala
transparent inline def map[B](inline f: A => B): Maybe[B] =
    if isEmpty then Absent else f(get)

transparent inline def flatMap[B](inline f: A => Maybe[B]): Maybe[B] =
    if isEmpty then Maybe.empty else f(get)
```

### 6. Pattern Matching Support
Pattern matching is supported through `unapply` methods, allowing:
```scala
maybe match
    case Maybe.Present(value) => // handle value
    case Maybe.Absent => // handle empty
```

## Critical Implementation Details for PrimitiveOption

1. **Use opaque types**: This is essential for zero allocation
2. **Implement PresentAbsent mechanism**: This solves the nested option problem
3. **All methods must be `transparent inline`**: For performance
4. **Handle null values in constructors**: Convert to Absent
5. **Track nesting depth**: Essential for correct semantics
6. **Pattern matching extractors**: Must handle the encoding properly

## Performance Characteristics

- Zero allocations for primitive types and non-nested options
- Single allocation (PresentAbsent) for nested empty values only
- All operations inlined at compile time
- No boxing for specialized types