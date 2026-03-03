import axios from 'axios';
import authService from './authService';

const API_BASE_URL = 'http://localhost:8080/api/v1';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use(
    async (config: any) => {
        const token = await authService.getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

export interface User {
    id: string;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    roles: string[];
}

export const iamApi = {
    getUsers: async (params?: any): Promise<User[]> => {
        const response = await api.get('/users', { params });
        return response.data as any;
    },

    getUser: async (id: string): Promise<User> => {
        const response = await api.get(`/users/${id}`);
        return response.data as any;
    },

    createUser: async (user: Partial<User>): Promise<User> => {
        const response = await api.post('/users', user);
        return response.data as any;
    },

    updateUser: async (id: string, user: Partial<User>): Promise<User> => {
        const response = await api.put(`/users/${id}`, user);
        return response.data as any;
    },

    deleteUser: async (id: string): Promise<void> => {
        await api.delete(`/users/${id}`);
    }
};
