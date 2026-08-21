import { FormEvent, useEffect, useState } from "react";
import * as workOrdersApi from "../api/workOrders";
import * as customersApi from "../api/customers";
import * as sitesApi from "../api/sites";
import { apiErrorMessage } from "../api/client";
import DataTable, { type Column } from "../components/DataTable";
import type { Customer, Site, WorkOrder, WorkOrderRequest } from "../types";

const emptyForm: WorkOrderRequest={title:"",description:"",customerId:0,siteId:0};

export default function WorkOrdersPage(){
 const [rows,setRows]=useState<WorkOrder[]>([]);
 const [customers,setCustomers]=useState<Customer[]>([]);
 const [sites,setSites]=useState<Site[]>([]);
 const [form,setForm]=useState<WorkOrderRequest>(emptyForm);
 const [loading,setLoading]=useState(true);
 const [error,setError]=useState<string|null>(null);
 const [formError,setFormError]=useState<string|null>(null);
 const [submitting,setSubmitting]=useState(false);

 async function load(){
  setLoading(true); setError(null);
  try { const [wo,cu]=await Promise.all([workOrdersApi.listWorkOrders(),customersApi.listCustomers({page:0,size:100})]);
    setRows(wo.content); setCustomers(cu.content);
  } catch(e){setError(apiErrorMessage(e,"Could not load work orders."));}
  finally{setLoading(false);}
 }
 useEffect(()=>{load();},[]);
 useEffect(()=>{
  if(!form.customerId){setSites([]); return;}
  sitesApi.listSites(form.customerId).then(setSites).catch(e=>setFormError(apiErrorMessage(e,"Could not load sites.")));
 },[form.customerId]);

 async function submit(e:FormEvent){e.preventDefault();setFormError(null);
  if(!form.customerId||!form.siteId){setFormError("Please select a customer and site.");return;}
  setSubmitting(true);
  try{await workOrdersApi.createWorkOrder(form);setForm(emptyForm);setSites([]);await load();}
  catch(e){setFormError(apiErrorMessage(e,"Could not create work order."));}
  finally{setSubmitting(false);}
 }
 const columns:Column<WorkOrder>[]=[
  {header:"ID",render:r=>r.id},{header:"Title",render:r=>r.title},
  {header:"Customer",render:r=>r.customerName},{header:"Site",render:r=>r.siteName},
  {header:"Status",render:r=>r.status},{header:"Created",render:r=>new Date(r.createdAt).toLocaleString()}
 ];
 return <div className="page-stack">
  <div className="page-header"><h1>Work Orders</h1><p className="page-header__subtitle">Create and view service work orders.</p></div>
  <section className="panel"><h2>Create work order</h2>
   <form className="inline-form" onSubmit={submit}>
    <input required placeholder="Title" value={form.title} onChange={e=>setForm(f=>({...f,title:e.target.value}))}/>
    <input placeholder="Description" value={form.description} onChange={e=>setForm(f=>({...f,description:e.target.value}))}/>
    <select required value={form.customerId||""} onChange={e=>setForm(f=>({...f,customerId:Number(e.target.value),siteId:0}))}>
      <option value="">Select customer</option>{customers.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
    </select>
    <select required disabled={!form.customerId} value={form.siteId||""} onChange={e=>setForm(f=>({...f,siteId:Number(e.target.value)}))}>
      <option value="">Select site</option>{sites.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
    </select>
    <button className="btn btn--primary" disabled={submitting}>{submitting?"Creating…":"Create"}</button>
   </form>{formError&&<div className="form-error">{formError}</div>}
  </section>
  <section className="panel"><h2>All work orders</h2><DataTable columns={columns} rows={rows} rowKey={r=>r.id} loading={loading} error={error}/></section>
 </div>;
}
