export const loadProfile = authorized => authorized('/profile');
export const updateProfile = (authorized, body) => authorized('/profile', { method: 'PUT', body });