class Permissions {
    constructor(public permissions: string[]) {}

    isAdmin() {
        return this.permissions.lastIndexOf("*") != -1;
    }

    isPermitted(permissions: string[]) {
        return this.isAdmin() || permissions.every((permission) => {
                return this.permissions.some((p) => p.toLocaleUpperCase() === permission.toLocaleUpperCase())
            });
    }
}

export = Permissions;