import SwiftUI

public struct ManageUsersView: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var users: [User] = [
        User(id: 1, username: "mansoor", displayName: "Mansoor", role: "Super Admin"),
        User(id: 2, username: "abdulqadir", displayName: "Abdul Qadir", role: "Cashier")
    ]
    @State private var newUsername: String = ""
    @State private var newDisplayName: String = ""

    public var body: some View {
        NavigationView {
            List {
                Section(header: Text("CURRENT USERS")) {
                    ForEach(users) { u in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(u.displayName).font(.headline)
                                Text("@\(u.username)").font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Text(u.role)
                                .font(.caption2)
                                .bold()
                                .padding(4)
                                .background(Color.blue.opacity(0.1))
                                .cornerRadius(4)
                        }
                    }
                }

                Section(header: Text("ADD USER")) {
                    TextField("Username", text: $newUsername)
                    TextField("Full Name", text: $newDisplayName)
                    Button("Add Cashier / Staff") {
                        if !newUsername.isEmpty {
                            users.append(User(id: users.count + 1, username: newUsername, displayName: newDisplayName, role: "Staff"))
                            newUsername = ""
                            newDisplayName = ""
                        }
                    }
                    .disabled(newUsername.isEmpty)
                }
            }
            .navigationTitle("Manage Users")
            .navigationBarItems(trailing: Button("Done") {
                presentationMode.wrappedValue.dismiss()
            })
        }
    }
}

