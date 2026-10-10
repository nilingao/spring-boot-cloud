package cn.com.nla.common.video.core.service.authentication;

/** Supplies the current caller from the consuming application's authentication context.
 * @author TZY
 */
@FunctionalInterface
public interface CurrentUserProvider {
    Long getUserId();
}
