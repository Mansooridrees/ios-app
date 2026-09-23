import Foundation

public struct User: Identifiable, Codable, Equatable {
    public var id: Int
    public var username: String
    public var displayName: String
    public var role: String

    public init(id: Int, username: String, displayName: String, role: String = "Super Admin") {
        self.id = id
        self.username = username
        self.displayName = displayName
        self.role = role
    }
}

