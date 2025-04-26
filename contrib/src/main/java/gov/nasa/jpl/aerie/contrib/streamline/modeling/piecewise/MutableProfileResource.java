package gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;

public interface MutableProfileResource<D extends Dynamics<?, D>> extends ProfileResource<D> {
    void emit(ProfileDynamicsEffect<D> effect);
    default void emit(ProfileDynamicsEffect<D> effect, String nameFormat, Object... nameArgs) {
        emit(name(effect, nameFormat, nameArgs));
    }

    static <D extends Dynamics<?, D>> MutableProfileResource<D> resource(ErrorCatching<Profile<D>> initial, EffectTrait<ProfileDynamicsEffect<D>> effectTrait) {
        MutableProfileResource<D> result = new MutableProfileResource<>() {
            private final CellRef<ProfileDynamicsEffect<D>, ProfileCell<D>> cell = CellRef.allocate(
                    new ProfileCell<>(initial),
                    new CellType<>() {
                        @Override
                        public EffectTrait<ProfileDynamicsEffect<D>> getEffectType() {
                            return effectTrait;
                        }

                        @Override
                        public ProfileCell<D> duplicate(ProfileCell<D> cell) {
                            return new ProfileCell<>(cell.profile);
                        }

                        @Override
                        public void apply(ProfileCell<D> cell, ProfileDynamicsEffect<D> effect) {
                            // TODO: Need to apply regularization to use the profile dynamics' inherent step,
                            //  instead of the stepping derived from effect application.
                            cell.profile = effect.apply(cell.profile);
                        }

                        @Override
                        public void step(ProfileCell<D> cell, Duration t) {
                            cell.profile = cell.profile.map(p -> p.step(t));
                        }

                        @Override
                        public Optional<Duration> getExpiry(ProfileCell<D> cell) {
                            return cell.profile.match(
                                    profile -> profile.dynamics().expiry().value(),
                                    failure -> Optional.empty());
                        }
                    }
            );

            @Override
            public ErrorCatching<Profile<D>> getDynamics() {
                return cell.get().profile;
            }

            @Override
            public void emit(ProfileDynamicsEffect<D> effect) {
                cell.emit(effect);
            }
        };
        // TODO: profiling and debugging
        return result;
    }
}
