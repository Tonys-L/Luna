# Luna Context

The core domain language for the Luna dynamic injection and diagnosis agent.

## Language

**InjectionRepository**:
The storage module responsible for saving and loading `PersistentInjection` records. It performs CRUD operations and persistence, with no knowledge of bytecode compilation or class matching.
_Avoid_: InjectionStore, RulePersistenceService

**InjectionRegistry**:
The in-memory registry that holds actively compiled `InjectionPoint`s. It is responsible for resolving wildcard/pattern matching of class names and is queried directly by the class file transformer during class loading.
_Avoid_: pointCache, classIndex

**InjectionService**:
The orchestration layer that coordinates validation, storage (via `InjectionRepository`), compilation, caching (via `InjectionRegistry`), and JVM retransformation.
_Avoid_: InjectionManager

## Relationships

- An **InjectionService** saves configurations to an **InjectionRepository**.
- An **InjectionService** compiles rules and registers them with the **InjectionRegistry**.
- The `GlobalClassFileTransformer` queries the **InjectionRegistry** to find active `InjectionPoint`s.
