package com.mycompany.archivosbinarios;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JOptionPane;

public class EmpleadoManager {
    private RandomAccessFile rcods,remps;
    
    /*
    RandomAccessFile crea el archivo si no existe sin necesidad de usar una funcion.
    */
    
    public EmpleadoManager(){
        try{
            //1- Asegurar que el folder comany exista
            File f=new File("company");
            f.mkdir();
            
            //2. Instanciar los RAFs dentro folder company
            rcods=new RandomAccessFile("company/codigos.emp","rw");
            remps=new RandomAccessFile("company/empleados.emp","rw");
            
            //3- Inicializar el archivo de codigos si es nuevo
            initCode();
        }catch(IOException e){
            System.out.println("No deberia para esto");
        }
    }//fin constructor
    
    private void initCode()throws  IOException{
        if(rcods.length()==0){
            //0 bytes
            rcods.writeInt(1);
            //4 bytes
        }   
    }
    
    private int getCode()throws IOException{
        //seek(ing) - mueve el puntero a una posicion indicada
        //getFilePointer() - indica donde esta el puntero
        rcods.seek(0);
        int code=rcods.readInt();//i
        rcods.seek(0);
        rcods.writeInt(code+1);
        return code;
    }
    
    public void addEmployee(String name,double salary)throws IOException{
        /*
        Formato de Empleados.emp
        codigo - int
        nombre - String
        salario - double
        fecha Contratacion - Fecha del momento - long
        fecha Despido - Fecha del momento de despido - long
        */
        //Asegurar que el puntero este al final
        remps.seek(remps.length());
        //codigo
        int code=getCode();
        remps.writeInt(code);
        //nombre
        remps.writeUTF(name);
        //salary
        remps.writeDouble(salary);
        //Fecha contratacion
        remps.writeLong(Calendar.getInstance().getTimeInMillis());
        //Fecha Despedir
        remps.writeLong(0);
        //Asegurar sus archivos individuales
        crearEmployeeFolders(code);
            
    }
    
    private String employeeFolder(int code){
        return "company/empleado"+code;
    }
    
    private void crearEmployeeFolders(int code) throws IOException{
        //Crear folder de empleado+ code
        File femp=new File(employeeFolder(code));
        femp.mkdir();
        //Crear los archivos de venta del empleado
        createYearSalesFilefor(code);
    }
    
    private RandomAccessFile salesFilefor(int code)throws IOException{
        String dirPadre=employeeFolder(code);
        int yearActual=Calendar.getInstance().get(Calendar.YEAR);
        String path=dirPadre+"/ventas"+yearActual+"emp";
        
        return new RandomAccessFile(path,"rw");
    }
    
    private void createYearSalesFilefor(int code) throws IOException{
        RandomAccessFile ryear=salesFilefor(code);
        /*
        Formato VentasYear.emp
        Ventas - Double
        estado - Boolean
        */
        
        if(ryear.length()==0){
            for(int mes=0;mes<12;mes++){
                ryear.writeDouble(0);
                ryear.writeBoolean(false);
            }
        }
        
    }
    
    public void employeeList()throws IOException{
        String list="";
 remps.seek(0);
 while(remps.getFilePointer()<remps.length()){
    int code=remps.readInt();
    String name=remps.readUTF();
    double salary= remps.readDouble();
    long fechaC=remps.readLong();
    Date fecha= new Date(fechaC);
    
    
    if(remps.readLong()==0){
        list+=("Codigo del empleado: "+code
                +" Nombre del empleado: "+name
                +" Salario: Lps. "+salary
                +" Fecha Contratacion: "+fecha+"\n");
    }
    }
 JOptionPane.showMessageDialog(null, list);
    }

    private boolean isEmployeeActive(int code) throws IOException{
        remps.seek(0);
        while(remps.getFilePointer()<remps.length()){
            int cod=remps.readInt();
            Long pos=remps.getFilePointer();
            remps.readUTF();
            remps.skipBytes(16);
            if(remps.readLong()==0 && cod==code){
                remps.seek(pos);
                return true;
            }
        }
        return false;
    }
    
    public boolean fireEmployee (int code) throws IOException{
        if(isEmployeeActive(code)){
            String name=remps.readUTF();
            remps.skipBytes(16);
            remps.writeLong(new Date().getTime());
            JOptionPane.showMessageDialog(null, "Despedir a: "+name);
            return true;
        }
        return false;
    }
    
    public void addSaletoEmployee(int code, double venta)throws IOException{
        if(isEmployeeActive(code)){
            RandomAccessFile sales= salesFilefor(code);
            int pos=Calendar.getInstance().get(Calendar.MONTH);
            sales.seek(pos);
            double montoA=sales.readDouble()+venta;
            sales.seek(pos);
            sales.writeDouble(montoA);
        }else{
            JOptionPane.showMessageDialog(null,"Empleado no Existe");
        }
    }
    
    private RandomAccessFile billsFileFor(int code)throws IOException{
        String dirPadre=employeeFolder(code);
        String path=dirPadre+"/recibos.emp";
        return new RandomAccessFile(path,"rw");
    }
    
    public boolean isEmployeePayed(int code) throws IOException{
        RandomAccessFile sales=salesFilefor(code);
        int pos=Calendar.getInstance().get(Calendar.MONTH)*9;
        sales.seek(pos);
        sales.skipBytes(8);
        return sales.readBoolean();
    }
    
    public void payEmployee(int code)throws IOException{
        String msg="";
        double sal=0;
        if(isEmployeeActive(code)&& !isEmployeePayed(code)){
            RandomAccessFile sales=salesFilefor(code);
            int year= Calendar.getInstance().get(Calendar.YEAR);
            int month=Calendar.getInstance().get(Calendar.MONTH);
            int pos=month*9;
            sales.seek(pos);
            
            String name=remps.readUTF();
            sal=remps.readDouble();
            
            double ventas=sales.readDouble();
            double sueldo=sal+(ventas*0.1);
            double deduc=sueldo*0.035;
            double total=sueldo-deduc;
            RandomAccessFile recibidos=billsFileFor(code);
            recibidos.seek(recibidos.length());
            recibidos.writeLong(Calendar.getInstance().getTimeInMillis());
            recibidos.writeDouble(sueldo);
            recibidos.writeDouble(deduc);
            recibidos.write(year);
            recibidos.write(month);
            sales.writeBoolean(true);
            msg=("Empleado: "+name
                +" se le pago Lps. "+total);
            JOptionPane.showMessageDialog(null, msg);
            
        }else{
            JOptionPane.showMessageDialog(null, "Error");
        }
    }
    
}
