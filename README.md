# Online Banking System

## Project Description
The Online Banking System is a Java Swing-based GUI application that provides a comprehensive banking experience. Users can create accounts, securely log in, perform transactions such as deposits, withdrawals, and transfers, view statements, apply interest, and manage account settings. Data is persisted using CSV files for accounts and transactions.

## Team Members
This project was developed by a dedicated team:
- **Shubham Kumar**: Lead Developer and Project Coordinator
- **Shyama**: UI/UX Designer and Frontend Developer
- **Komal Jha**: Backend Developer and Tester

## Features
- **Account Management**: Create new accounts with validation, secure login with SHA-256 hashed passwords.
- **Transactions**: Deposit, withdraw, and transfer funds between accounts.
- **Statements**: View mini-statements (last 5 transactions) or full transaction history.
- **Interest Application**: Apply simple interest (4% p.a.) on a daily pro-rated basis.
- **Settings**: Change password with confirmation.
- **Data Persistence**: All data is stored in CSV files (`bank_data/accounts.csv` and `bank_data/transactions.csv`).
- **Modern UI**: Nimbus look and feel with custom colors, icons, tooltips, and hover effects.

## Prerequisites
- Java Development Kit (JDK) 8 or higher installed on your system.
- Basic knowledge of running Java applications from the command line.

## How to Run
1. Ensure you have Java installed. You can check by running `java -version` in your terminal.
2. Navigate to the project directory: `cd Online_Banking_System`
3. Compile the application: `javac BankAppGUI.java`
4. Run the application: `java BankAppGUI`

The application will launch a GUI window for the Online Banking System.

## Project Structure
- `BankAppGUI.java`: Main application file containing the GUI and business logic.
- `bank_data/`: Directory for data persistence.
  - `accounts.csv`: Stores account information.
  - `transactions.csv`: Stores transaction records.
- Other `.class` files: Compiled Java classes.

## Usage
- **Login**: Enter your username and password to access your account.
- **Register**: Create a new account if you don't have one.
- **Dashboard**: After login, use the tabs to perform various banking operations.
- **Logout**: Always logout to save your data.

## Contributing
This is a team project. For contributions or modifications, please coordinate with the team members.

## License
This project is for educational purposes. Please refer to the team for any licensing inquiries.

