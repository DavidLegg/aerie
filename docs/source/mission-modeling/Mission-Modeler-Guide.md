# Introduction

This guide explains how to make use of Aerie-provided capabilities in the latest version of Aerie, and will be updated as Aerie evolves.

Aerie is a new software system being developed by the MPSA element of MGSS (Multi-mission Ground System and Services), a subsystem of AMMOS (Advanced Multi-mission Operations System). Aerie will support mission operations by providing capabilities for activity planning, sequencing, and spacecraft analysis. These capabilities include modeling & simulation, scheduling, and validation. Aerie will replace several legacy MGSS tools, including but not limited to APGEN, SEQGEN, MPS Editor and MPS Server.

Aerie currently provides the following elements.

- Merlin modeling framework for defining mission resources and activity types.
- Merlin web UI for activity planning and simulation analysis.
- Falcon sequence editor UI

# Mission Modeling with the Merlin Framework

In Merlin, a mission model serves activity planning needs in two ways. First, it describes how various mission resources behave autonomously over time. Second, it defines how activities perturb these resources at discrete time points, causing them to change their behavior. This information enables Aerie to provide scheduling, constraint validation, and resource plotting capabilities on top of a mission model.

The Merlin Framework empowers adaptation engineers to serve the needs of mission planners maintain as well as keep their codebase maintainable and testable over the span of a mission. The Framework aims to make the experience of mission modeling similar to standard Java development, while still addressing the unique needs of the simulation domain.

In the Merlin Framework, a mission model breaks down into two types of entity: system models and activity types.

System models can range in complexity from a single aspect of an instrument to an entire mission. In fact, Merlin only requires one system model to exist: the top-level mission model. The mission model can delegate to other, more focused models, such as subsystem models, which may themselves delegate further. Ultimately, fine-grained models capture the system state they own in a `Cell`, which is a simulation-aware analogue of a Java mutable field. (**In Merlin, mutable fields on models must not be used.** All mutable state must be controlled by a `Cell`.) Models may provide regular Java methods for interacting with that state, and other models (including activity types) may invoke those methods.

Activity types are a specialized kind of model. Each activity type defines the parameters for activities of that type, which may be instantiated and configured by a mission planner. An activity type also defines a single method that acts as the entrypoint into the simulated system: when an activity of that type occurs, its method is invoked with the activity parameters and the top-level mission model. It may then interact freely with the rest of the system.

Just as activity types define the entrypoints into a simulation, the mission model also defines _resources_, which allow information to be extracted from the simulation. A resource is assoociated with a method that returns a "dynamics" -- a description of the current autonomous behavior of the resource. Merlin currently provides discrete dynamics (constants held over time) and linear dynamics (real values varying linearly with time), and is designed to support more in the future.

A simulation over a mission model iteratively runs activities and queries resources for their updated dynamics, producing a composite profile of dynamics for each resource over the entire simulation duration.