/**
 * Public MarieUI facade API.
 *
 * <p>This package holds the slim, static entry-point classes consumer mods are meant to call
 * without reading marie-ui's internals — {@code MarieNotifications}, {@code MarieCommandCenter},
 * {@code MarieScaleConfig}, {@code EditModeCoordinator}, and their future peers. Each facade's
 * actual implementation (rendering, hit-testing, persistence, registries) stays in its own
 * subsystem package (e.g. {@code dev.marie.framework.notification}, {@code
 * dev.marie.framework.ui.commandcenter}, {@code dev.marie.framework.ui.scaleconfig}, {@code
 * dev.marie.framework.ui.edit}); only the front door lives here. Mirrors marie-core's {@code
 * dev.marie.framework.api} package, which plays the same role for MarieLib's non-UI surface.
 */
@ApiStatus.Experimental
package dev.marie.framework.ui.api;

import dev.marie.framework.api.ApiStatus;
