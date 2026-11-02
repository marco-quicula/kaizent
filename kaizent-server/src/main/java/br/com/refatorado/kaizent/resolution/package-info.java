/**
 * Resolution, the Do step: turns a problem into a {@link
 * br.com.refatorado.kaizent.resolution.Proposal}. In spec 0002 this is a single model call ({@link
 * br.com.refatorado.kaizent.resolution.SkeletonResolver}); spec 0008 replaces it with the agent
 * loop.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Resolution")
package br.com.refatorado.kaizent.resolution;
